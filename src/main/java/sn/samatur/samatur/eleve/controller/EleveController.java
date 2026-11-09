package sn.samatur.samatur.eleve.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sn.samatur.samatur.eleve.dto.EleveDto;
import sn.samatur.samatur.eleve.dto.RapportImport;
import sn.samatur.samatur.eleve.service.EleveService;
import sn.samatur.samatur.eleve.service.ImportCsvService;
import sn.samatur.samatur.security.UserDetailsImpl;

import java.util.List;

/**
 * Les élèves arrivent par l'import CSV (F1) : le contrat de base ne prévoit ni création,
 * ni modification, ni suppression unitaire (décision du 09/11).
 */
@RestController
@RequestMapping("/api/eleves")
@Tag(name = "Élèves")
@SecurityRequirement(name = "bearerAuth")
public class EleveController {

    private final EleveService eleveService;
    private final ImportCsvService importCsvService;

    public EleveController(EleveService eleveService, ImportCsvService importCsvService) {
        this.eleveService = eleveService;
        this.importCsvService = importCsvService;
    }

    @GetMapping
    @Operation(summary = "Lister les élèves")
    @PreAuthorize("hasAnyRole('ECOLE', 'SUPERVISEUR')")
    public List<EleveDto> listerTous() {
        return eleveService.listerTous();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un élève")
    @PreAuthorize("hasAnyRole('ECOLE', 'SUPERVISEUR')")
    public EleveDto trouverParId(@PathVariable Long id) {
        return eleveService.trouverParId(id);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer des élèves depuis un CSV",
            description = "Fichier CSV UTF-8 dans le champ multipart `file`, séparateur point-virgule, 2 Mo et "
                    + "1 000 lignes au maximum. Colonnes obligatoires : prenom, nom, sexe, date_naissance (JJ/MM/AAAA), "
                    + "commune_naissance, code_ecole, classe, nom_mere ; facultatives : date_naissance_presumee "
                    + "(oui/non), nom_pere, telephone_parent (+221 et 9 chiffres), langue_contact, numero_ine. "
                    + "Chaque ligne valide crée un élève et son dossier DETECTE ; le rapport donne le motif de "
                    + "chaque ligne rejetée.")
    @PreAuthorize("hasRole('ECOLE')")
    public RapportImport importerEleves(@RequestParam("file") MultipartFile file,
                                        @AuthenticationPrincipal UserDetailsImpl qui) {
        return importCsvService.importer(file, qui);
    }
}
