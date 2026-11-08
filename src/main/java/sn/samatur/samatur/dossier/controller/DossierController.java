package sn.samatur.samatur.dossier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.service.DossierService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/dossiers")
@Tag(name = "Dossiers")
@SecurityRequirement(name = "bearerAuth")
public class DossierController {

    @Autowired
    private DossierService dossierService;

    // 1. Lister tous les dossiers
    @GetMapping
    @Operation(summary = "Lister les dossiers")
    @PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL', 'SUPERVISEUR')")
    public ResponseEntity<List<Dossier>> listerTous() {
        return ResponseEntity.ok(dossierService.listerTous());
    }

    // 2. Trouver un dossier par son ID
    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un dossier")
    @PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL', 'SUPERVISEUR')")
    public ResponseEntity<Dossier> trouverParId(@PathVariable Long id) {
        return dossierService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Faire avancer un dossier � l'�tape suivante (Machine � �tats)
    @PostMapping("/{id}/avancer")
    @Operation(summary = "Faire avancer un dossier", description = "Déplace le dossier vers l'étape suivante et enregistre la transition.")
    @PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL')")
    public ResponseEntity<Dossier> avancerEtape(@PathVariable Long id, Principal principal) {
        String auteur = (principal != null) ? principal.getName() : "Inconnu";
        try {
            Dossier dossierMisAJour = dossierService.avancerEtape(id, auteur);
            return ResponseEntity.ok(dossierMisAJour);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
