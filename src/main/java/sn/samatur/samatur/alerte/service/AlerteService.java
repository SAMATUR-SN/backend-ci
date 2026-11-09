package sn.samatur.samatur.alerte.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.alerte.dto.AlerteDto;
import sn.samatur.samatur.alerte.entity.Alerte;
import sn.samatur.samatur.alerte.repository.AlerteRepository;
import sn.samatur.samatur.commun.RessourceIntrouvableException;
import sn.samatur.samatur.dossier.dto.DossierResumeDto;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.repository.DossierRepository;
import sn.samatur.samatur.dossier.repository.DossierSpecifications;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Alertes de retard (F5). Un dossier est en retard s'il est à JUGEMENT_RENDU et que la date du jour
 * dépasse la date du jugement plus le délai cible (RG-05) : c'est le blocage entre le tribunal et la
 * mairie que Sama Tur surveille. Le retard se calcule à la lecture ; la table alerte ne trace que la
 * première détection de chaque dossier et sa résolution (RG-11). Le délai de 15 jours est une cible
 * du projet, pas un délai légal (RG-06).
 */
@Service
public class AlerteService {

    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final DossierRepository dossiers;
    private final AlerteRepository alertes;
    private final Clock horloge;
    private final int delaiCible;

    public AlerteService(DossierRepository dossiers, AlerteRepository alertes, Clock horloge,
                         @Value("${samatur.delai-cible-jours:15}") int delaiCible) {
        this.dossiers = dossiers;
        this.alertes = alertes;
        this.horloge = horloge;
        this.delaiCible = delaiCible;
    }

    private LocalDate limite() {
        return LocalDate.now(horloge).minusDays(delaiCible);
    }

    /** Les dossiers en retard, du plus ancien jugement au plus récent. */
    @Transactional(readOnly = true)
    public List<Dossier> dossiersEnRetard() {
        return dossiers.findByEtapeAndDateJugementBeforeOrderByDateJugementAsc(StatutDossier.JUGEMENT_RENDU, limite());
    }

    /** GET /api/alertes : les dossiers en retard en résumés, filtrés par commune de naissance si demandé. */
    @Transactional(readOnly = true)
    public List<DossierResumeDto> listerEnRetard(Long communeId) {
        Specification<Dossier> spec = Specification.allOf(
                DossierSpecifications.enRetard(true, limite()),
                DossierSpecifications.commune(communeId));
        LocalDate aujourdhui = LocalDate.now(horloge);
        return dossiers.findAll(spec, Sort.by("dateJugement").ascending().and(Sort.by("id")))
                .stream().map(d -> DossierResumeDto.de(d, true, aujourdhui)).toList();
    }

    /** Appelée par AlerteScheduler chaque nuit, ou par POST /api/alertes/verifier. Renvoie le nombre d'alertes créées. */
    @Transactional
    public int enregistrerNouvellesAlertes() {
        int creees = 0;
        for (Dossier d : dossiersEnRetard()) {
            if (alertes.existsByDossierId(d.getId())) {
                continue;   // RG-11 : une alerte au plus par dossier, même après sa résolution
            }
            Alerte a = new Alerte();
            a.setDossier(d);
            a.setMessage("Jugement du " + d.getDateJugement().format(FR)
                    + " non transcrit après le délai cible de " + delaiCible + " jours.");
            a.setDateAlerte(LocalDateTime.now(horloge));
            alertes.save(a);
            creees++;
        }
        return creees;
    }

    /** Marque une alerte comme résolue. */
    @Transactional
    public AlerteDto resoudre(Long id) {
        Alerte alerte = alertes.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Alerte " + id + " introuvable."));
        alerte.setResolue(true);
        return AlerteDto.de(alerte);
    }
}
