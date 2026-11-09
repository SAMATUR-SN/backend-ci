package sn.samatur.samatur.commun;

/** Dossier ou référentiel inexistant. Répond 404. */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
