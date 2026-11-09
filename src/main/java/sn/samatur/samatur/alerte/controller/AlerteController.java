package sn.samatur.samatur.alerte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.samatur.samatur.alerte.dto.AlerteDto;
import sn.samatur.samatur.alerte.service.AlerteService;
import sn.samatur.samatur.dossier.dto.DossierResumeDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertes")
@Tag(name = "Alertes")
@SecurityRequirement(name = "bearerAuth")
public class AlerteController {

    private final AlerteService alerteService;

    public AlerteController(AlerteService alerteService) {
        this.alerteService = alerteService;
    }

    @GetMapping
    @Operation(summary = "Lister les dossiers en retard",
            description = "Jugements rendus et non transcrits au-delà du délai cible (15 jours par défaut, "
                    + "samatur.delai-cible-jours). C'est une cible du projet, pas un délai légal. "
                    + "Du plus ancien jugement au plus récent.")
    public List<DossierResumeDto> listerDossiersEnRetard(
            @Parameter(description = "Commune de naissance de l'élève") @RequestParam(required = false) Long communeId) {
        return alerteService.listerEnRetard(communeId);
    }

    @PostMapping("/verifier")
    @PreAuthorize("hasRole('ETAT_CIVIL')")
    @Operation(summary = "Enregistrer les alertes des nouveaux retards",
            description = "Ce que fait la tâche planifiée chaque nuit : une alerte au plus par dossier en retard.")
    public Map<String, Integer> declencherVerificationManuelle() {
        return Map.of("alertesCreees", alerteService.enregistrerNouvellesAlertes());
    }

    @PatchMapping("/{id}/resoudre")
    @PreAuthorize("hasRole('ETAT_CIVIL')")
    @Operation(summary = "Résoudre une alerte")
    public AlerteDto resoudreAlerte(@PathVariable Long id) {
        return alerteService.resoudre(id);
    }
}
