package sn.samatur.samatur.security.dto;

/**
 * L'utilisateur connecté tel que le frontend l'affiche : rôle et nom de sa structure
 * (école, centre d'état civil ou tribunal ; vide pour le superviseur).
 */
public record UtilisateurDto(String identifiant, String nomAffiche, String role, String structure) {
}
