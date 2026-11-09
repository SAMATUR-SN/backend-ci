package sn.samatur.samatur.commun;

/** Conflit avec l'état actuel : doublon, étape déjà franchie, dossier terminé. Répond 409. */
public class ConflitException extends RuntimeException {

    public ConflitException(String message) {
        super(message);
    }
}
