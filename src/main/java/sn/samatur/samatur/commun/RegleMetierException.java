package sn.samatur.samatur.commun;

import java.util.List;

/** Règle métier violée : champ obligatoire, saut d'étape, dates incohérentes. Répond 422. */
public class RegleMetierException extends RuntimeException {

    private final String code;
    private final List<String> champs;

    /**
     * @param code   fin de l'URI du type d'erreur (https://samatur.sn/erreurs/{code})
     * @param champs champs de la requête concernés, vide si la règle porte sur le dossier entier
     */
    public RegleMetierException(String code, String message, List<String> champs) {
        super(message);
        this.code = code;
        this.champs = List.copyOf(champs);
    }

    public String code() {
        return code;
    }

    public List<String> champs() {
        return champs;
    }
}
