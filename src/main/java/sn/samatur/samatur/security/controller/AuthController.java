package sn.samatur.samatur.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sn.samatur.samatur.security.JwtUtils;
import sn.samatur.samatur.security.ProfilService;
import sn.samatur.samatur.security.UserDetailsImpl;
import sn.samatur.samatur.security.dto.LoginRequest;
import sn.samatur.samatur.security.dto.ReponseConnexion;
import sn.samatur.samatur.security.dto.UtilisateurDto;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final ProfilService profils;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils, ProfilService profils) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.profils = profils;
    }

    @PostMapping("/connexion")
    @SecurityRequirements
    @Operation(summary = "Se connecter",
            description = "Renvoie le jeton à envoyer dans l'en-tête Authorization: Bearer, sa date d'expiration "
                    + "et l'utilisateur. Identifiant inconnu, mauvais mot de passe ou compte désactivé : 401, "
                    + "toujours avec le même message.")
    public ResponseEntity<ReponseConnexion> connexion(@Valid @RequestBody LoginRequest demande) {
        Authentication authentification = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(demande.getIdentifiant(), demande.getMotDePasse()));
        UserDetailsImpl qui = (UserDetailsImpl) authentification.getPrincipal();
        if (qui == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        JwtUtils.JetonEmis jeton = jwtUtils.emettre(qui);
        return ResponseEntity.ok(new ReponseConnexion(
                jeton.jeton(), jeton.expireA(), profils.profil(qui.getUsername())));
    }

    @GetMapping("/moi")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Utilisateur connecté", description = "Rôle et structure de rattachement, lus en base.")
    public UtilisateurDto moi(@AuthenticationPrincipal UserDetailsImpl qui) {
        return profils.profil(qui.getUsername());
    }
}
