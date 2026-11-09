package sn.samatur.samatur.eleve.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.commun.ConflitException;
import sn.samatur.samatur.commun.PageDto;
import sn.samatur.samatur.commun.RegleMetierException;
import sn.samatur.samatur.commun.RessourceIntrouvableException;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.repository.DossierRepository;
import sn.samatur.samatur.dossier.service.DossierService;
import sn.samatur.samatur.eleve.dto.EleveDto;
import sn.samatur.samatur.eleve.dto.EleveRequest;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.eleve.entity.Langue;
import sn.samatur.samatur.eleve.repository.EleveRepository;
import sn.samatur.samatur.referentiel.entity.Commune;
import sn.samatur.samatur.referentiel.repository.CommuneRepository;
import sn.samatur.samatur.referentiel.repository.EcoleRepository;
import sn.samatur.samatur.security.UserDetailsImpl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Gestion des élèves par le directeur d'école (fonction facultative, après le contrat de base).
 * Tout est limité à l'école du compte connecté : un élève d'une autre école est introuvable (404),
 * pour ne pas révéler son existence. Un élève n'est jamais supprimé physiquement (RG-13) : il est archivé.
 */
@Service
public class EleveService {

    private final EleveRepository eleveRepository;
    private final EcoleRepository ecoles;
    private final CommuneRepository communes;
    private final DossierRepository dossiers;
    private final DossierService dossierService;
    private final Clock horloge;

    public EleveService(EleveRepository eleveRepository, EcoleRepository ecoles, CommuneRepository communes,
                        DossierRepository dossiers, DossierService dossierService, Clock horloge) {
        this.eleveRepository = eleveRepository;
        this.ecoles = ecoles;
        this.communes = communes;
        this.dossiers = dossiers;
        this.dossierService = dossierService;
        this.horloge = horloge;
    }

    /** Les élèves actifs de l'école, par nom puis prénom. */
    @Transactional(readOnly = true)
    public PageDto<EleveDto> lister(UserDetailsImpl qui, int page, int taille) {
        var pageable = PageRequest.of(page, Math.min(taille, 100), Sort.by("nom", "prenom", "id"));
        return PageDto.de(eleveRepository.findByEcoleIdAndArchiveLeIsNull(ecoleDe(qui), pageable).map(EleveDto::de));
    }

    @Transactional(readOnly = true)
    public EleveDto trouver(Long id, UserDetailsImpl qui) {
        return EleveDto.de(trouverDansEcole(id, qui));
    }

    /**
     * Crée l'élève dans l'école du compte connecté, puis son dossier DETECTE et sa première ligne
     * d'historique, par la même méthode que l'import CSV.
     */
    @Transactional
    public EleveDto creer(EleveRequest r, UserDetailsImpl qui) {
        Eleve e = new Eleve();
        e.setEcole(ecoles.getReferenceById(ecoleDe(qui)));
        appliquer(e, r);
        if (eleveRepository.existsByPrenomIgnoreCaseAndNomIgnoreCaseAndDateNaissanceAndEcoleIdAndArchiveLeIsNull(
                e.getPrenom(), e.getNom(), e.getDateNaissance(), ecoleDe(qui))) {
            throw new ConflitException("Un élève de même prénom, nom, date de naissance et école existe déjà.");  // RG-07
        }
        verifierIne(e.getNumeroIne(), null);
        eleveRepository.save(e);
        dossierService.creerDossierDetecte(e, qui, "Dossier créé par saisie du directeur d'école");
        return EleveDto.de(e);
    }

    /**
     * Modifie l'identité et le contact. Règle proposée, à valider par Dri : une fois le dossier au-delà
     * de DETECTE, nom, prénom et date de naissance ne changent plus (409), car le certificat et le
     * jugement ont été établis à cette identité.
     */
    @Transactional
    public EleveDto modifier(Long id, EleveRequest r, UserDetailsImpl qui) {
        Eleve e = trouverDansEcole(id, qui);
        boolean identiteModifiee = !Objects.equals(e.getNom(), r.nom().trim())
                || !Objects.equals(e.getPrenom(), r.prenom().trim())
                || !Objects.equals(e.getDateNaissance(), r.dateNaissance());
        boolean dossierAvance = dossiers.findByEleveId(id)
                .map(d -> d.getEtape() != StatutDossier.DETECTE).orElse(false);
        if (identiteModifiee && dossierAvance) {
            throw new ConflitException("Le dossier a dépassé l'étape DETECTE : nom, prénom et date de naissance "
                    + "ne peuvent plus changer.");
        }
        if (eleveRepository.existsByPrenomIgnoreCaseAndNomIgnoreCaseAndDateNaissanceAndEcoleIdAndArchiveLeIsNullAndIdNot(
                r.prenom().trim(), r.nom().trim(), r.dateNaissance(), ecoleDe(qui), id)) {
            throw new ConflitException("Un élève de même prénom, nom, date de naissance et école existe déjà.");  // RG-07
        }
        verifierIne(r.numeroIne(), id);
        appliquer(e, r);
        return EleveDto.de(e);
    }

    /** Archive l'élève : il sort, avec son dossier, des listes, des alertes et des indicateurs ; rien n'est effacé. */
    @Transactional
    public void archiver(Long id, UserDetailsImpl qui) {
        trouverDansEcole(id, qui).setArchiveLe(LocalDateTime.now(horloge));
    }

    private void appliquer(Eleve e, EleveRequest r) {
        if (!r.dateNaissance().isBefore(LocalDate.now(horloge))) {
            throw new RegleMetierException("date-future", "La date de naissance doit être dans le passé.",
                    List.of("dateNaissance"));
        }
        Commune commune = communes.findById(r.communeNaissanceId())
                .orElseThrow(() -> new RegleMetierException("commune-inconnue", "Commune de naissance inconnue.",
                        List.of("communeNaissanceId")));
        e.setPrenom(r.prenom().trim());
        e.setNom(r.nom().trim());
        e.setSexe(r.sexe());
        e.setDateNaissance(r.dateNaissance());
        e.setDateNaissancePresumee(Boolean.TRUE.equals(r.dateNaissancePresumee()));
        e.setCommuneNaissance(commune);
        e.setClasse(r.classe().name());
        e.setNomMere(r.nomMere().trim());
        e.setNomPere(videEnNull(r.nomPere()));
        e.setTelephoneParent(videEnNull(r.telephoneParent()));
        e.setLangueContact(r.langueContact() != null ? r.langueContact() : Langue.FRANCAIS);
        e.setNumeroIne(videEnNull(r.numeroIne()));
    }

    private void verifierIne(String ine, Long idModifie) {
        String valeur = videEnNull(ine);
        if (valeur != null && eleveRepository.findByNumeroIne(valeur)
                .filter(autre -> !autre.getId().equals(idModifie)).isPresent()) {
            throw new ConflitException("Numéro INE déjà utilisé.");
        }
    }

    private Eleve trouverDansEcole(Long id, UserDetailsImpl qui) {
        return eleveRepository.findByIdAndEcoleIdAndArchiveLeIsNull(id, ecoleDe(qui))
                .orElseThrow(() -> new RessourceIntrouvableException("Élève " + id + " introuvable."));
    }

    private static Long ecoleDe(UserDetailsImpl qui) {
        if (qui.getEcoleId() == null) {
            throw new AccessDeniedException("Ce compte n'est rattaché à aucune école.");
        }
        return qui.getEcoleId();
    }

    private static String videEnNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
