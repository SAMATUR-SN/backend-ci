package sn.samatur.samatur.dossier.entity;

import java.util.Optional;

/**
 * Les six étapes du dossier, dans l'ordre du cycle de vie (cahier des charges, section 3).
 * Mêmes valeurs que la contrainte CHECK de la colonne dossier.etape (V1).
 * Chaque étape connaît le rôle autorisé à y faire entrer un dossier.
 */
public enum StatutDossier {
    DETECTE("ECOLE"),
    CERTIFICAT_OBTENU("ETAT_CIVIL"),
    DOSSIER_COMPLET("ECOLE"),
    JUGEMENT_RENDU("TRIBUNAL"),
    TRANSCRIT("ETAT_CIVIL"),
    ACTE_REMIS("ECOLE");

    private final String roleAutorise;

    StatutDossier(String roleAutorise) {
        this.roleAutorise = roleAutorise;
    }

    /** Rôle qui fait entrer un dossier dans cette étape (RG-03). */
    public String roleAutorise() {
        return roleAutorise;
    }

    /** L'étape suivante, ou vide si le dossier est terminé. */
    public Optional<StatutDossier> suivante() {
        StatutDossier[] toutes = values();
        int i = ordinal() + 1;
        return i < toutes.length ? Optional.of(toutes[i]) : Optional.empty();
    }
}
