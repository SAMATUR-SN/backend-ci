package sn.samatur.samatur.commun;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Gestionnaire d'erreurs unique de l'API : toutes les erreurs sortent au format
 * application/problem+json (RFC 9457), sans trace Java ni message SQL.
 * Pas de gestionnaire générique Exception.class : il transformerait aussi les refus
 * de sécurité en erreurs 500.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    static final String ECHEC_CONNEXION = "Identifiant ou mot de passe incorrect.";

    /**
     * RG-15 : même réponse que l'identifiant soit inconnu, le mot de passe faux ou le compte
     * désactivé, pour ne pas révéler quels comptes existent.
     */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    ProblemDetail connexionRefusee() {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ECHEC_CONNEXION);
        p.setType(URI.create("https://samatur.sn/erreurs/connexion-refusee"));
        p.setTitle("Connexion refusée");
        return p;
    }

    /** Connecté, mais rôle non autorisé pour cette action (RG-14). */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesRefuse() {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Votre rôle ne permet pas cette action.");
        p.setType(URI.create("https://samatur.sn/erreurs/acces-refuse"));
        p.setTitle("Accès refusé");
        return p;
    }
}
