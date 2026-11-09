package sn.samatur.samatur.dossier.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sn.samatur.samatur.commun.PageDto;
import sn.samatur.samatur.dossier.dto.AvancerDossierRequest;
import sn.samatur.samatur.dossier.dto.DossierDetailDto;
import sn.samatur.samatur.dossier.dto.DossierResumeDto;
import sn.samatur.samatur.dossier.dto.FiltreDossiers;
import sn.samatur.samatur.dossier.service.DossierService;
import sn.samatur.samatur.security.UserDetailsImpl;

@RestController
@RequestMapping("/api/dossiers")
@Tag(name = "Dossiers")
@SecurityRequirement(name = "bearerAuth")
public class DossierController {

    private final DossierService dossierService;

    public DossierController(DossierService dossierService) {
        this.dossierService = dossierService;
    }

    @GetMapping
    @Operation(summary = "Lister les dossiers, paginés et filtrés",
            description = "Filtres facultatifs : etape, communeId, ecoleId, classe, enRetard, q. "
                    + "taille est plafonnée à 100 ; tri vaut par exemple dateDetection,desc (par défaut), "
                    + "reference,asc ou dateJugement,asc.")
    @PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL', 'SUPERVISEUR')")
    public PageDto<DossierResumeDto> lister(@ParameterObject FiltreDossiers filtre,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) int taille,
                                            @RequestParam(required = false) String tri) {
        return dossierService.lister(filtre, page, taille, tri);
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
