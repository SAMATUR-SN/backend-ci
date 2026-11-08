package sn.samatur.samatur.statistiques.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.samatur.samatur.statistiques.service.StatistiquesService;

import java.util.Map;

@RestController
@RequestMapping("/api/statistiques")
@Tag(name = "Statistiques")
@SecurityRequirement(name = "bearerAuth")
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    @Autowired
    public StatistiquesController(StatistiquesService statistiquesService) {
        this.statistiquesService = statistiquesService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Obtenir les indicateurs du tableau de bord")
    public ResponseEntity obtenirDashboard() {
        Map<String, Object> stats = statistiquesService.obtenirTableauDeBord();
        return ResponseEntity.ok(stats);
    }
}