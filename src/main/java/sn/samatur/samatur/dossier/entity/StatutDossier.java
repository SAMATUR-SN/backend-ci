package sn.samatur.samatur.dossier.entity;

/**
 * Les six étapes du dossier, dans l'ordre du cycle de vie (cahier des charges, section 3).
 * Mêmes valeurs que la contrainte CHECK de la colonne dossier.etape (V1).
 */
public enum StatutDossier {
    DETECTE,
    CERTIFICAT_OBTENU,
    DOSSIER_COMPLET,
    JUGEMENT_RENDU,
    TRANSCRIT,
    ACTE_REMIS
}
