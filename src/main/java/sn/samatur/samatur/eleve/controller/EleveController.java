package sn.samatur.samatur.eleve.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sn.samatur.samatur.eleve.dto.EleveDto;
import sn.samatur.samatur.eleve.service.EleveService;

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

    public EleveController(EleveService eleveService) {
        this.eleveService = eleveService;
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

    @PostMapping("/import")
    @Operation(summary = "Importer des élèves depuis un CSV",
            description = "Le fichier multipart est fourni dans le champ `file` ; séparateur point-virgule.")
    @PreAuthorize("hasRole('ECOLE')")
    public ResponseEntity<?> importerEleves(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Veuillez fournir un fichier CSV valide.");
        }
        List<String> erreurs = eleveService.importerCsv(file);
        if (erreurs.isEmpty()) {
            return ResponseEntity.ok("Importation des élèves réussie avec succès !");
        } else {
            return ResponseEntity.badRequest().body(erreurs);
        }
    }
}
