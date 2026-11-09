package sn.samatur.samatur.dossier.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sn.samatur.samatur.dossier.dto.AvancerDossierRequest;
import sn.samatur.samatur.dossier.dto.DossierDetailDto;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.service.DossierService;
import sn.samatur.samatur.security.UserDetailsImpl;

import java.util.List;

@RestController
@RequestMapping("/api/dossiers")
@Tag(name = "Dossiers")
@SecurityRequirement(name = "bearerAuth")
public class DossierController {

    private final DossierService dossierService;

    public DossierController(DossierService dossierService) {
        this.dossierService = dossierService;
    }

    // Lister tous les dossiers (paginé, filtré et en DTO au bloc 5)
    @GetMapping
    @Operation(summary = "Lister les dossiers")
    @PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL', 'SUPERVISEUR')")
    public ResponseEntity<List<Dossier>> listerTous() {
        return ResponseEntity.ok(dossierService.listerTous());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un dossier",
            description = "Élève, étape courante, données de chaque étape, témoins et historique horodaté.")
    @PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL', 'SUPERVISEUR')")
    public DossierDetailDto trouverParId(@PathVariable Long id) {
        return dossierService.detail(id);
    }

    @PostMapping("/{id}/avancer")
    @Operation(summary = "Faire passer un dossier à l'étape suivante",
            description = "etapeCible doit être l'étape suivant l'étape courante, et la requête porter les champs "
                    + "exigés par cette étape. Saut d'étape ou champ manquant : 422 ; étape déjà franchie : 409 ; "
                    + "rôle non autorisé pour l'étape : 403.")
    @PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL')")
    public DossierDetailDto avancer(@PathVariable Long id,
                                    @Valid @RequestBody AvancerDossierRequest req,
                                    @AuthenticationPrincipal UserDetailsImpl qui) {
        return dossierService.avancer(id, req, qui);
    }
}
