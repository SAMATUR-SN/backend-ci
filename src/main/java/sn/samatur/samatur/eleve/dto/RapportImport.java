package sn.samatur.samatur.eleve.dto;

import java.util.List;

/**
 * Rapport d'un import CSV (F1) : {"total": 20, "importes": 18, "rejetes": 2,
 * "erreurs": [{"ligne": 7, "motif": "commune inconnue : Bambeye"}]}.
 * Les numéros de ligne sont ceux du fichier, en-tête compris : la première ligne d'élève est la ligne 2.
 */
public record RapportImport(int total, int importes, int rejetes, List<ErreurLigne> erreurs) {

    public record ErreurLigne(int ligne, String motif) {
    }
}
