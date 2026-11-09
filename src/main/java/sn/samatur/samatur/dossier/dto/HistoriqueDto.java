package sn.samatur.samatur.dossier.dto;

import sn.samatur.samatur.dossier.entity.StatutDossier;

import java.time.LocalDateTime;

/** Une ligne d'historique : qui a fait passer le dossier d'une étape à l'autre, et quand. */
public record HistoriqueDto(StatutDossier etapeDepart, StatutDossier etapeArrivee, LocalDateTime dateHeure,
                            String auteur, String role, String commentaire) {
}
