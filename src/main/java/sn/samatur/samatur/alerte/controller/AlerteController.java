package sn.samatur.samatur.alerte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.samatur.samatur.alerte.entity.Alerte;
import sn.samatur.samatur.alerte.repository.AlerteRepository;
import sn.samatur.samatur.alerte.service.AlerteService;

import java.util.List;

@RestController
@RequestMapping("/api/alertes")
@Tag(name = "Alertes")
@SecurityRequirement(name = "bearerAuth")
public class AlerteController {

    @Autowired
    private AlerteRepository alerteRepository;

    @Autowired
    private AlerteService alerteService;

    @GetMapping
    @Operation(summary = "Lister les alertes actives")
    public ResponseEntity<List<Alerte>> listerAlertesActives() {
        List<Alerte> alertes = alerteRepository.findByResolueFalse();
        return ResponseEntity.ok(alertes);
    }

    @PostMapping("/verifier")
    @PreAuthorize("hasRole('ETAT_CIVIL')")
    @Operation(summary = "Déclencher la vérification des retards")
    public ResponseEntity<String> declencherVerificationManuelle() {
        alerteService.verifierRetardsDossiers();
        return ResponseEntity.ok("V\u00e9rification des retards ex\u00e9cut\u00e9e avec succ\u00e8s.");
    }

    @PatchMapping("/{id}/resoudre")
    @PreAuthorize("hasRole('ETAT_CIVIL')")
    @Operation(summary = "Résoudre une alerte")
    public ResponseEntity<Alerte> resoudreAlerte(@PathVariable Long id) {
        Alerte alerte = alerteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerte introuvable avec l'id : " + id));
        
        alerte.setResolue(true);
        Alerte alerteSauvegardee = alerteRepository.save(alerte);
        
        return ResponseEntity.ok(alerteSauvegardee);
    }
}
