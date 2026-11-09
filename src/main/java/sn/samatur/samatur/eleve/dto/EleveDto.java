package sn.samatur.samatur.eleve.dto;

import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.eleve.entity.Langue;

import java.time.LocalDate;

/** L'élève tel que l'API l'expose : jamais l'entité JPA elle-même. */
public record EleveDto(
        Long id,
        String numeroIne,
        String prenom,
        String nom,
        String sexe,
        LocalDate dateNaissance,
        Boolean dateNaissancePresumee,
        String classe,
        Long ecoleId,
        String ecole,
        Long communeNaissanceId,
        String communeNaissance,
        String nomPere,
        String nomMere,
        String telephoneParent,
        Langue langueContact) {

    /** À appeler dans une transaction : l'école et la commune sont chargées à la demande. */
    public static EleveDto de(Eleve e) {
        return new EleveDto(e.getId(), e.getNumeroIne(), e.getPrenom(), e.getNom(), e.getSexe(),
                e.getDateNaissance(), e.getDateNaissancePresumee(), e.getClasse(),
                e.getEcole() != null ? e.getEcole().getId() : null,
                e.getEcole() != null ? e.getEcole().getNom() : null,
                e.getCommuneNaissance() != null ? e.getCommuneNaissance().getId() : null,
                e.getCommuneNaissance() != null ? e.getCommuneNaissance().getNom() : null,
                e.getNomPere(), e.getNomMere(), e.getTelephoneParent(), e.getLangueContact());
    }
}
