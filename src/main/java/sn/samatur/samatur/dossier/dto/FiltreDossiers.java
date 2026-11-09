package sn.samatur.samatur.dossier.dto;

import io.swagger.v3.oas.annotations.Parameter;
import sn.samatur.samatur.dossier.entity.StatutDossier;

/** Filtres de la liste des dossiers (F2). Chacun est facultatif ; absent, il ne filtre rien. */
public record FiltreDossiers(
        @Parameter(description = "Étape courante du dossier") StatutDossier etape,
        @Parameter(description = "Commune de naissance de l'élève") Long communeId,
        @Parameter(description = "École de l'élève") Long ecoleId,
        @Parameter(description = "Classe de l'élève (PRESCOLAIRE, CI, CP, CE1, CE2, CM1, CM2)") String classe,
        @Parameter(description = "true : jugement non transcrit au-delà du délai cible ; false : les autres")
        Boolean enRetard,
        @Parameter(description = "Recherche dans la référence, le nom et le prénom") String q) {
}
