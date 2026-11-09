package sn.samatur.samatur.security.dto;

import java.time.Instant;

/** Réponse de POST /api/auth/connexion, au format du contrat d'API. */
public record ReponseConnexion(String jeton, Instant expireA, UtilisateurDto utilisateur) {
}
