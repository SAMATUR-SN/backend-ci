package sn.samatur.samatur.alerte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.samatur.samatur.alerte.dto.AlerteDto;
import sn.samatur.samatur.alerte.service.AlerteService;

import java.util.List;

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
    @Operation(summary = "Lister les alertes actives")
    public List<AlerteDto> listerAlertesActives() {
        return alerteService.listerActives();
    }

    @PostMapping("/verifier")
    @PreAuthorize("hasRole('ETAT_CIVIL')")
    @Operation(summary = "Déclencher la vérification des retards")
    public ResponseEntity<String> declencherVerificationManuelle() {
        alerteService.verifierRetardsDossiers();
        return ResponseEntity.ok("Vérification des retards exécutée.");
    }

    @PatchMapping("/{id}/resoudre")
    @PreAuthorize("hasRole('ETAT_CIVIL')")
    @Operation(summary = "Résoudre une alerte")
    public AlerteDto resoudreAlerte(@PathVariable Long id) {
        return alerteService.resoudre(id);
    }
}
