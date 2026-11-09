package sn.samatur.samatur.dossier.dto;

import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.eleve.entity.Eleve;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/** Un dossier dans une liste, au format du contrat d'API (cahier des charges, section 6). */
public record DossierResumeDto(
        Long id,
        String reference,
        EleveResumeDto eleve,
        String communeNaissance,
        StatutDossier etape,
        LocalDate dateDerniereEtape,
        boolean enRetard,
        Integer joursDepuisJugement) {

    public record EleveResumeDto(String prenom, String nom, String classe, String ecole) {
    }

    /** À appeler dans une transaction : l'école et la commune de l'élève sont chargées à la demande. */
    public static DossierResumeDto de(Dossier d, boolean enRetard, LocalDate aujourdhui) {
        Eleve e = d.getEleve();
        // La date la plus récente parmi celles des étapes franchies (DOSSIER_COMPLET n'a pas de date propre)
        LocalDate derniere = Stream.of(d.getDateRemise(), d.getDateTranscription(), d.getDateJugement(),
                        d.getDateCertificat(), d.getDateDetection())
                .filter(date -> date != null).findFirst().orElse(null);
        Integer jours = d.getDateJugement() == null ? null
                : (int) ChronoUnit.DAYS.between(d.getDateJugement(), aujourdhui);
        return new DossierResumeDto(d.getId(), d.getReference(),
                new EleveResumeDto(e.getPrenom(), e.getNom(), e.getClasse(),
                        e.getEcole() != null ? e.getEcole().getNom() : null),
                e.getCommuneNaissance() != null ? e.getCommuneNaissance().getNom() : null,
                d.getEtape(), derniere, enRetard, jours);
    }
}
