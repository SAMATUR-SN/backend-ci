package sn.samatur.samatur.eleve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import sn.samatur.samatur.eleve.entity.Classe;
import sn.samatur.samatur.eleve.entity.Langue;

import java.time.LocalDate;

/**
 * Saisie ou modification d'un élève par le directeur d'école (fonction facultative).
 * Mêmes champs et mêmes règles que l'import CSV ; l'école est celle du compte connecté,
 * jamais une valeur envoyée par le client.
 */
public record EleveRequest(
        @NotBlank @Size(max = 150) String prenom,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Pattern(regexp = "[FM]", message = "F ou M attendu") String sexe,
        @NotNull LocalDate dateNaissance,
        Boolean dateNaissancePresumee,
        @NotNull Long communeNaissanceId,
        @NotNull Classe classe,
        @NotBlank @Size(max = 150) String nomMere,
        @Size(max = 150) String nomPere,
        @Pattern(regexp = "\\+221\\d{9}", message = "+221 suivi de 9 chiffres attendu") String telephoneParent,
        Langue langueContact,
        @Size(max = 50) String numeroIne) {
}
