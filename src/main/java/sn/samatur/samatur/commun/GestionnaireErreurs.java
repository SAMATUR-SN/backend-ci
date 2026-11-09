package sn.samatur.samatur.commun;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;

/**
 * Gestionnaire d'erreurs unique de l'API : toutes les erreurs sortent au format
 * application/problem+json (RFC 9457), sans trace Java ni message SQL.
 * Il hérite de ResponseEntityExceptionHandler pour que les erreurs standard de Spring MVC
 * (JSON illisible, paramètre mal typé…) aient le même format.
 * Pas de gestionnaire générique Exception.class : il transformerait aussi les refus
 * de sécurité en erreurs 500.
 */
@RestControllerAdvice
public class GestionnaireErreurs extends ResponseEntityExceptionHandler {

    static final String ECHEC_CONNEXION = "Identifiant ou mot de passe incorrect.";
    private static final String TYPE = "https://samatur.sn/erreurs/";

    private static ProblemDetail probleme(HttpStatus statut, String code, String titre, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(statut, detail);
        p.setType(URI.create(TYPE + code));
        p.setTitle(titre);
        return p;
    }

    /** Un champ en erreur, sérialisé dans cet ordre : {"champ": …, "message": …}. */
    record ErreurChamp(String champ, String message) {
    }

    private static List<ErreurChamp> erreurs(List<String> champs, String message) {
        return champs.stream().map(c -> new ErreurChamp(c, message)).toList();
    }

    /** 422 : champ obligatoire manquant, saut d'étape, dates incohérentes (RG-02, RG-03, RG-04). */
    @ExceptionHandler(RegleMetierException.class)
    ProblemDetail regleMetier(RegleMetierException e) {
        boolean champManquant = "champ-obligatoire".equals(e.code());
        ProblemDetail p = probleme(HttpStatus.UNPROCESSABLE_CONTENT, e.code(),
                champManquant ? "Champ obligatoire manquant" : "Règle métier non respectée", e.getMessage());
        p.setProperty("erreurs", erreurs(e.champs(), champManquant ? "obligatoire" : "invalide"));
        return p;
    }

    /** 409 : étape déjà franchie, doublon, ou dossier modifié entre-temps par quelqu'un d'autre. */
    @ExceptionHandler(ConflitException.class)
    ProblemDetail conflit(ConflitException e) {
        return probleme(HttpStatus.CONFLICT, "conflit", "Conflit", e.getMessage());
    }

    /** 409 sans exposer le message de la base, qui contient du SQL. */
    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    ProblemDetail conflitDonnees() {
        return probleme(HttpStatus.CONFLICT, "conflit", "Conflit",
                "La donnée existe déjà ou a été modifiée entre-temps.");
    }

    /** 404 : dossier ou référentiel inexistant. */
    @ExceptionHandler(RessourceIntrouvableException.class)
    ProblemDetail introuvable(RessourceIntrouvableException e) {
        return probleme(HttpStatus.NOT_FOUND, "introuvable", "Ressource introuvable", e.getMessage());
    }

    /**
     * 401, RG-15 : même réponse que l'identifiant soit inconnu, le mot de passe faux ou le compte
     * désactivé, pour ne pas révéler quels comptes existent.
     */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    ProblemDetail connexionRefusee() {
        return probleme(HttpStatus.UNAUTHORIZED, "connexion-refusee", "Connexion refusée", ECHEC_CONNEXION);
    }

    /** 403 : connecté, mais rôle non autorisé pour cette action (RG-03, RG-14). */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesRefuse(AccessDeniedException e) {
        return probleme(HttpStatus.FORBIDDEN, "acces-refuse", "Accès refusé",
                e.getMessage() != null && e.getMessage().startsWith("Rôle non autorisé")
                        ? e.getMessage() : "Votre rôle ne permet pas cette action.");
    }

    /** 422 : un champ validé par Bean Validation (@NotNull…) est absent ou invalide. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErreurChamp> liste = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErreurChamp(f.getField(), messageDe(f))).toList();
        ProblemDetail p = probleme(HttpStatus.UNPROCESSABLE_CONTENT, "champ-obligatoire",
                "Champ obligatoire manquant", "La requête est incomplète ou invalide.");
        p.setProperty("erreurs", liste);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(p);
    }

    private static String messageDe(FieldError f) {
        String code = f.getCode();
        return "NotNull".equals(code) || "NotBlank".equals(code) || "NotEmpty".equals(code)
                ? "obligatoire" : "invalide";
    }

    /** 400 : corps JSON illisible, ou valeur inconnue pour un enum (étape, type d'audience…). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail p = probleme(HttpStatus.BAD_REQUEST, "requete-illisible", "Requête illisible",
                "Le corps de la requête n'est pas un JSON valide, ou contient une valeur inconnue.");
        return ResponseEntity.badRequest().body(p);
    }
}
