package sn.samatur.samatur.dossier.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.samatur.samatur.commun.ConflitException;
import sn.samatur.samatur.commun.PageDto;
import sn.samatur.samatur.commun.RegleMetierException;
import sn.samatur.samatur.commun.RessourceIntrouvableException;
import sn.samatur.samatur.dossier.dto.AvancerDossierRequest;
import sn.samatur.samatur.dossier.dto.DossierDetailDto;
import sn.samatur.samatur.dossier.dto.DossierResumeDto;
import sn.samatur.samatur.dossier.dto.FiltreDossiers;
import sn.samatur.samatur.dossier.dto.HistoriqueDto;
import sn.samatur.samatur.dossier.dto.TemoinDto;
import sn.samatur.samatur.dossier.repository.DossierSpecifications;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.HistoriqueEtape;
import sn.samatur.samatur.dossier.entity.PreuveNaissance;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.entity.Temoin;
import sn.samatur.samatur.dossier.repository.DossierRepository;
import sn.samatur.samatur.dossier.repository.HistoriqueEtapeRepository;
import sn.samatur.samatur.eleve.dto.EleveDto;
import sn.samatur.samatur.referentiel.entity.CentreEtatCivil;
import sn.samatur.samatur.referentiel.entity.TribunalInstance;
import sn.samatur.samatur.referentiel.repository.CentreEtatCivilRepository;
import sn.samatur.samatur.referentiel.repository.TribunalInstanceRepository;
import sn.samatur.samatur.security.UserDetailsImpl;
import sn.samatur.samatur.security.entity.Utilisateur;
import sn.samatur.samatur.security.repository.UtilisateurRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Le cœur de Sama Tur : faire avancer un dossier dans ses six étapes en refusant
 * tout ce qui viole une règle du cahier des charges (section 3, RG-02 à RG-05, RG-09).
 */
@Service
public class DossierService {

    private final DossierRepository dossierRepository;
    private final HistoriqueEtapeRepository historiqueEtapeRepository;
    private final CentreEtatCivilRepository centres;
    private final TribunalInstanceRepository tribunaux;
    private final UtilisateurRepository utilisateurs;
    private final Clock horloge;
    private final int delaiCibleJours;

    public DossierService(DossierRepository dossierRepository,
                          HistoriqueEtapeRepository historiqueEtapeRepository,
                          CentreEtatCivilRepository centres,
                          TribunalInstanceRepository tribunaux,
                          UtilisateurRepository utilisateurs,
                          Clock horloge,
                          @Value("${samatur.delai-cible-jours:15}") int delaiCibleJours) {
        this.dossierRepository = dossierRepository;
        this.historiqueEtapeRepository = historiqueEtapeRepository;
        this.centres = centres;
        this.tribunaux = tribunaux;
        this.utilisateurs = utilisateurs;
        this.horloge = horloge;
        this.delaiCibleJours = delaiCibleJours;
    }

    /** Champs de tri acceptés par le paramètre tri (« champ,asc » ou « champ,desc »). */
    private static final Map<String, String> TRIS = Map.of(
            "reference", "reference",
            "dateDetection", "dateDetection",
            "dateJugement", "dateJugement");

    /**
     * F2 : liste paginée et filtrée. Les DTO sont construits dans la transaction, sinon
     * les relations chargées à la demande (école, commune) échoueraient hors session.
     */
    @Transactional(readOnly = true)
    public PageDto<DossierResumeDto> lister(FiltreDossiers f, int page, int taille, String tri) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        Specification<Dossier> spec = Specification.allOf(
                DossierSpecifications.etape(f.etape()),
                DossierSpecifications.commune(f.communeId()),
                DossierSpecifications.ecole(f.ecoleId()),
                DossierSpecifications.classe(f.classe()),
                DossierSpecifications.enRetard(f.enRetard(), aujourdhui.minusDays(delaiCibleJours)),
                DossierSpecifications.recherche(f.q()));
        PageRequest pageable = PageRequest.of(page, Math.min(taille, 100), ordre(tri).and(Sort.by("id")));
        return PageDto.de(dossierRepository.findAll(spec, pageable)
                .map(d -> DossierResumeDto.de(d, estEnRetard(d), aujourdhui)));
    }

    private static Sort ordre(String tri) {
        if (tri == null || tri.isBlank()) {
            return Sort.by("dateDetection").descending();
        }
        String[] morceaux = tri.split(",");
        String champ = TRIS.get(morceaux[0].trim());
        String sens = morceaux.length > 1 ? morceaux[1].trim().toLowerCase() : "asc";
        if (champ == null || !(sens.equals("asc") || sens.equals("desc"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tri inconnu : utiliser reference, dateDetection ou dateJugement, suivi de ,asc ou ,desc.");
        }
        return Sort.by(Sort.Direction.fromString(sens), champ);
    }

    /** F3 : le dossier complet, avec ses témoins et son historique. */
    @Transactional(readOnly = true)
    public DossierDetailDto detail(Long id) {
        return versDetail(trouver(id));
    }

    /**
     * F4 : fait passer le dossier à l'étape suivante. La mise à jour du dossier et la ligne
     * d'historique sont enregistrées ensemble ou pas du tout (RG-09).
     */
    @Transactional
    public DossierDetailDto avancer(Long id, AvancerDossierRequest req, UserDetailsImpl qui) {
        Dossier d = trouver(id);
        StatutDossier depart = d.getEtape();
        StatutDossier attendue = depart.suivante()
                .orElseThrow(() -> new ConflitException("Ce dossier est déjà terminé."));

        // RG-02 : une étape à la fois, dans l'ordre ; un rejeu ne fait pas avancer deux fois
        if (req.etapeCible() != attendue) {
            if (req.etapeCible().ordinal() <= depart.ordinal()) {
                throw new ConflitException("Le dossier a déjà franchi l'étape " + req.etapeCible() + ".");
            }
            throw new RegleMetierException("saut-etape",
                    "Le dossier doit d'abord passer par " + attendue + ".", List.of());
        }
        // RG-03 et RG-16 : le rôle vient du compte connecté, jamais de la requête
        if (!attendue.roleAutorise().equals(qui.getRole())) {
            throw new AccessDeniedException("Rôle non autorisé pour l'étape " + attendue + ".");
        }

        switch (attendue) {
            case CERTIFICAT_OBTENU -> appliquerCertificat(d, req);
            case DOSSIER_COMPLET -> appliquerDossierComplet(d, req);
            case JUGEMENT_RENDU -> appliquerJugement(d, req);
            case TRANSCRIT -> appliquerTranscription(d, req);
            case ACTE_REMIS -> appliquerRemise(d, req);
            case DETECTE -> throw new IllegalStateException("DETECTE n'est jamais une étape suivante.");
        }
        verifierChronologie(d);
        d.setEtape(attendue);
        dossierRepository.saveAndFlush(d);

        HistoriqueEtape h = new HistoriqueEtape();
        h.setDossier(d);
        h.setEtapeDepart(depart);
        h.setEtapeArrivee(attendue);
        h.setDateHeure(LocalDateTime.now(horloge));
        h.setAuteurId(qui.getId());
        h.setRoleAuteur(qui.getRole());
        h.setCommentaire(req.commentaire());
        historiqueEtapeRepository.save(h);

        return versDetail(d);
    }

    private Dossier trouver(Long id) {
        return dossierRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dossier " + id + " introuvable."));
    }

    private void appliquerCertificat(Dossier d, AvancerDossierRequest r) {
        ChampsManquants manquants = new ChampsManquants();
        manquants.exiger("numeroCertificat", r.numeroCertificat());
        manquants.exiger("dateCertificat", r.dateCertificat());
        manquants.exiger("centreEtatCivilId", r.centreEtatCivilId());
        manquants.leverSiBesoin(StatutDossier.CERTIFICAT_OBTENU);

        CentreEtatCivil centre = centres.findById(r.centreEtatCivilId())
                .orElseThrow(() -> new RegleMetierException("centre-inconnu",
                        "Centre d'état civil inconnu.", List.of("centreEtatCivilId")));
        d.setNumeroCertificat(r.numeroCertificat().trim());
        d.setDateCertificat(r.dateCertificat());
        d.setCentreEtatCivil(centre);
    }

    private void appliquerDossierComplet(Dossier d, AvancerDossierRequest r) {
        ChampsManquants manquants = new ChampsManquants();
        manquants.exiger("preuveNaissance", r.preuveNaissance());
        manquants.leverSiBesoin(StatutDossier.DOSSIER_COMPLET);

        if (r.preuveNaissance() == PreuveNaissance.TEMOINS) {
            List<TemoinDto> temoins = r.temoins() == null ? List.of() : r.temoins();
            boolean complets = temoins.size() == 2 && temoins.stream().allMatch(t -> t != null
                    && ChampsManquants.renseigne(t.nomComplet()) && ChampsManquants.renseigne(t.numeroPiece()));
            if (!complets) {
                throw new RegleMetierException("temoins", "La preuve par témoins exige exactement deux témoins, "
                        + "chacun avec son nom complet et son numéro de pièce.", List.of("temoins"));
            }
            d.getTemoins().clear();
            for (TemoinDto t : temoins) {
                Temoin temoin = new Temoin();
                temoin.setDossier(d);
                temoin.setNomComplet(t.nomComplet().trim());
                temoin.setNumeroPiece(t.numeroPiece().trim());
                d.getTemoins().add(temoin);
            }
        }
        d.setPreuveNaissance(r.preuveNaissance());
    }

    private void appliquerJugement(Dossier d, AvancerDossierRequest r) {
        ChampsManquants manquants = new ChampsManquants();
        manquants.exiger("numeroJugement", r.numeroJugement());
        manquants.exiger("dateJugement", r.dateJugement());
        manquants.exiger("tribunalId", r.tribunalId());
        manquants.exiger("typeAudience", r.typeAudience());
        manquants.leverSiBesoin(StatutDossier.JUGEMENT_RENDU);

        TribunalInstance tribunal = tribunaux.findById(r.tribunalId())
                .orElseThrow(() -> new RegleMetierException("tribunal-inconnu",
                        "Tribunal inconnu.", List.of("tribunalId")));
        d.setNumeroJugement(r.numeroJugement().trim());
        d.setDateJugement(r.dateJugement());
        d.setTribunalInstance(tribunal);
        d.setTypeAudience(r.typeAudience());
    }

    private void appliquerTranscription(Dossier d, AvancerDossierRequest r) {
        ChampsManquants manquants = new ChampsManquants();
        manquants.exiger("numeroActe", r.numeroActe());
        manquants.exiger("anneeActe", r.anneeActe());
        manquants.exiger("dateTranscription", r.dateTranscription());
        manquants.leverSiBesoin(StatutDossier.TRANSCRIT);

        d.setNumeroActe(r.numeroActe().trim());
        d.setAnneeActe(r.anneeActe());
        d.setDateTranscription(r.dateTranscription());
    }

    private void appliquerRemise(Dossier d, AvancerDossierRequest r) {
        ChampsManquants manquants = new ChampsManquants();
        manquants.exiger("dateRemise", r.dateRemise());
        manquants.leverSiBesoin(StatutDossier.ACTE_REMIS);

        d.setDateRemise(r.dateRemise());
    }

    /** RG-04 : détection ≤ certificat ≤ jugement ≤ transcription ≤ remise, et aucune date dans le futur. */
    private void verifierChronologie(Dossier d) {
        List<LocalDate> dates = Stream.of(d.getDateDetection(), d.getDateCertificat(),
                        d.getDateJugement(), d.getDateTranscription(), d.getDateRemise())
                .filter(Objects::nonNull).toList();
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).isBefore(dates.get(i - 1))) {
                throw new RegleMetierException("dates-incoherentes",
                        "Les dates des étapes doivent se suivre : détection, certificat, jugement, "
                                + "transcription, remise.", List.of());
            }
        }
        LocalDate aujourdhui = LocalDate.now(horloge);
        if (dates.stream().anyMatch(date -> date.isAfter(aujourdhui))) {
            throw new RegleMetierException("date-future",
                    "Une date d'étape ne peut pas être dans le futur.", List.of());
        }
    }

    /**
     * RG-05 : en retard s'il est à JUGEMENT_RENDU et que la date du jour dépasse la date du
     * jugement plus le délai cible. Calculé à la lecture, jamais stocké.
     */
    public boolean estEnRetard(Dossier d) {
        return d.getEtape() == StatutDossier.JUGEMENT_RENDU && d.getDateJugement() != null
                && LocalDate.now(horloge).isAfter(d.getDateJugement().plusDays(delaiCibleJours));
    }

    private DossierDetailDto versDetail(Dossier d) {
        List<HistoriqueEtape> lignes = historiqueEtapeRepository.findByDossierIdOrderByDateHeureAscIdAsc(d.getId());
        List<Long> auteurs = lignes.stream().map(HistoriqueEtape::getAuteurId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> noms = utilisateurs.findAllById(auteurs).stream()
                .collect(Collectors.toMap(Utilisateur::getId, Utilisateur::getNomAffiche));
        List<HistoriqueDto> historique = lignes.stream()
                .map(h -> new HistoriqueDto(h.getEtapeDepart(), h.getEtapeArrivee(), h.getDateHeure(),
                        noms.get(h.getAuteurId()), h.getRoleAuteur(), h.getCommentaire()))
                .toList();

        Integer joursDepuisJugement = d.getDateJugement() == null ? null
                : (int) ChronoUnit.DAYS.between(d.getDateJugement(), LocalDate.now(horloge));
        CentreEtatCivil centre = d.getCentreEtatCivil();
        TribunalInstance tribunal = d.getTribunalInstance();
        return new DossierDetailDto(d.getId(), d.getReference(), d.getEtape(), estEnRetard(d), joursDepuisJugement,
                EleveDto.de(d.getEleve()), d.getDateDetection(),
                d.getNumeroCertificat(), d.getDateCertificat(),
                nullSinon(centre, CentreEtatCivil::getId), nullSinon(centre, CentreEtatCivil::getNom),
                d.getPreuveNaissance(), d.getTemoins().stream().map(TemoinDto::de).toList(),
                d.getNumeroJugement(), d.getDateJugement(),
                nullSinon(tribunal, TribunalInstance::getId), nullSinon(tribunal, TribunalInstance::getNom),
                d.getTypeAudience(), d.getNumeroActe(), d.getAnneeActe(), d.getDateTranscription(),
                d.getDateRemise(), historique);
    }

    private static <T, R> R nullSinon(T valeur, Function<T, R> lecture) {
        return valeur == null ? null : lecture.apply(valeur);
    }

    /** Accumule les champs absents d'une étape, puis refuse en 422 avec leur liste (RG-03). */
    static final class ChampsManquants {
        private final List<String> champs = new ArrayList<>();

        static boolean renseigne(Object valeur) {
            return valeur != null && !(valeur instanceof String s && s.isBlank());
        }

        void exiger(String nom, Object valeur) {
            if (!renseigne(valeur)) {
                champs.add(nom);
            }
        }

        void leverSiBesoin(StatutDossier etape) {
            if (!champs.isEmpty()) {
                throw new RegleMetierException("champ-obligatoire",
                        "Le passage à " + etape + " exige : " + String.join(", ", champs) + ".", champs);
            }
        }
    }
}
