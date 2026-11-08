package sn.samatur.samatur.dossier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.samatur.samatur.dossier.dto.CreationDossierRequest;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.HistoriqueEtape;
import sn.samatur.samatur.dossier.service.DossierService;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

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

    @PostMapping
    @Operation(summary = "Créer un dossier à partir d'un élève")
    @PreAuthorize("hasAnyRole('ECOLE', 'SUPERVISEUR')")
    public ResponseEntity<?> creerDossier(@Valid @RequestBody CreationDossierRequest request) {
        try {
            Dossier dossier = dossierService.creerDossier(request);
            return ResponseEntity.created(URI.create("/api/dossiers/" + dossier.getId())).body(dossier);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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

    @GetMapping("/{id}/historique")
    @Operation(summary = "Consulter l'historique d'un dossier")
    @PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL', 'SUPERVISEUR')")
    public ResponseEntity<List<HistoriqueEtape>> obtenirHistorique(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(dossierService.obtenirHistorique(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 3. Faire avancer un dossier à l'étape suivante.
    @PostMapping("/{id}/avancer")
    @Operation(summary = "Faire avancer un dossier", description = "Déplace le dossier vers l'étape suivante et enregistre la transition.")
    @PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL')")
    public ResponseEntity<?> avancerEtape(@PathVariable Long id) {
        try {
            Dossier dossierMisAJour = dossierService.avancerEtape(id);
            return ResponseEntity.ok(dossierMisAJour);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
