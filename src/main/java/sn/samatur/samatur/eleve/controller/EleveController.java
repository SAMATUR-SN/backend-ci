package sn.samatur.samatur.eleve.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.eleve.service.EleveService;

import java.util.List;

@RestController
@RequestMapping("/api/eleves")
@Tag(name = "Élèves")
@SecurityRequirement(name = "bearerAuth")
public class EleveController {

    @Autowired
    private EleveService eleveService;

    // 1. Lister tous les élèves
    @GetMapping
    @Operation(summary = "Lister les élèves")
    @PreAuthorize("hasAnyRole('ECOLE', 'SUPERVISEUR', 'ADMIN')")
    public ResponseEntity<List<Eleve>> listerTous() {
        List<Eleve> eleves = eleveService.listerTous();
        return ResponseEntity.ok(eleves);
    }

    // 2. Trouver un élève par son ID
    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un élève")
    @PreAuthorize("hasAnyRole('ECOLE', 'SUPERVISEUR', 'ADMIN')")
    public ResponseEntity<Eleve> trouverParId(@PathVariable Long id) {
        return eleveService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Créer un élève unitairement
    @PostMapping
    @Operation(summary = "Créer un élève")
    @PreAuthorize("hasAnyRole('ECOLE', 'SUPERVISEUR')")
    public ResponseEntity<Eleve> creerEleve(@RequestBody Eleve eleve) {
        Eleve nouvelEleve = eleveService.enregistrerEleve(eleve);
        return ResponseEntity.ok(nouvelEleve);
    }

    // 4. Modifier un élève existant
    @PutMapping("/{id}")
    @Operation(summary = "Modifier un élève")
    @PreAuthorize("hasAnyRole('ECOLE', 'SUPERVISEUR')")
    public ResponseEntity<Eleve> modifierEleve(@PathVariable Long id, @RequestBody Eleve eleveDetails) {
        try {
            Eleve eleveModifie = eleveService.modifierEleve(id, eleveDetails);
            return ResponseEntity.ok(eleveModifie);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 5. Supprimer un élève
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un élève")
    @PreAuthorize("hasAnyRole('ECOLE', 'SUPERVISEUR')")
    public ResponseEntity<Void> supprimerEleve(@PathVariable Long id) {
        try {
            eleveService.supprimerEleve(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 6. Importation CSV (Endpoint existant)
    @PostMapping("/import")
    @Operation(summary = "Importer des élèves depuis un CSV", description = "Le fichier multipart doit être fourni dans le champ `file`. Le séparateur CSV attendu est le point-virgule.")
    @PreAuthorize("hasAnyRole('ECOLE', 'SUPERVISEUR')")
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