package sn.samatur.samatur.statistiques.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.samatur.samatur.statistiques.dto.IndicateursDto;
import sn.samatur.samatur.statistiques.service.StatistiquesService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/indicateurs")
@Tag(name = "Indicateurs")
@SecurityRequirement(name = "bearerAuth")
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    public StatistiquesController(StatistiquesService statistiquesService) {
        this.statistiquesService = statistiquesService;
    }

    @GetMapping
    @Operation(summary = "Indicateurs du tableau de bord",
            description = "I1 : part des jugements transcrits dans le délai cible (entre 0 et 1, null sans dossier "
                    + "transcrit). I2 : délai médian en jours entre détection et acte remis (null sans acte remis). "
                    + "I3 : élèves de CM2 sans acte remis à la date de référence. Plus le nombre de dossiers par "
                    + "étape et de dossiers en retard.")
    public IndicateursDto obtenirIndicateurs(
            @Parameter(description = "Commune de naissance de l'élève") @RequestParam(required = false) Long communeId,
            @Parameter(description = "Date de référence d'I3 (AAAA-MM-JJ), aujourd'hui par défaut")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateReference) {
        return statistiquesService.calculer(communeId, dateReference);
    }
}
