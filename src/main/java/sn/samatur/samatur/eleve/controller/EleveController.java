package sn.samatur.samatur.eleve.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sn.samatur.samatur.commun.PageDto;
import sn.samatur.samatur.eleve.dto.EleveDto;
import sn.samatur.samatur.eleve.dto.EleveRequest;
import sn.samatur.samatur.eleve.dto.RapportImport;
import sn.samatur.samatur.eleve.service.EleveService;
import sn.samatur.samatur.eleve.service.ImportCsvService;
import sn.samatur.samatur.security.UserDetailsImpl;

/**
 * Les élèves de l'école du directeur connecté. Le contrat de base ne prévoit que l'import CSV ;
 * la liste, la saisie, la modification et l'archivage forment la fonction facultative.
 * Tout est réservé au rôle ECOLE et limité à sa propre école.
 */
@RestController
@RequestMapping("/api/eleves")
@Tag(name = "Élèves")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ECOLE')")
public class EleveController {

    private final EleveService eleveService;
    private final ImportCsvService importCsvService;

    public EleveController(EleveService eleveService, ImportCsvService importCsvService) {
        this.eleveService = eleveService;
        this.importCsvService = importCsvService;
    }

    @GetMapping
    @Operation(summary = "Lister les élèves de mon école", description = "Facultatif. Élèves actifs, par nom et prénom.")
    public PageDto<EleveDto> lister(@RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "20") @Min(1) int taille,
                                    @AuthenticationPrincipal UserDetailsImpl qui) {
        return eleveService.lister(qui, page, taille);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un élève de mon école", description = "Facultatif. Élève d'une autre école : 404.")
    public EleveDto trouver(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl qui) {
        return eleveService.trouver(id, qui);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Saisir un élève dans mon école",
            description = "Facultatif. Mêmes règles que l'import CSV ; crée aussi le dossier DETECTE. Doublon : 409.")
    public EleveDto creer(@Valid @RequestBody EleveRequest requete, @AuthenticationPrincipal UserDetailsImpl qui) {
        return eleveService.creer(requete, qui);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier l'identité et le contact d'un élève",
            description = "Facultatif. Une fois le dossier au-delà de DETECTE, nom, prénom, date de naissance et "
                    + "commune de naissance ne changent plus : 409.")
    public EleveDto modifier(@PathVariable Long id, @Valid @RequestBody EleveRequest requete,
                             @AuthenticationPrincipal UserDetailsImpl qui) {
        return eleveService.modifier(id, requete, qui);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archiver un élève",
            description = "Facultatif. Archivage, jamais une suppression physique (RG-13) : l'élève et son dossier "
                    + "sortent des listes, des alertes et des indicateurs ; l'historique est conservé. Seulement "
                    + "tant que le dossier est à DETECTE : au-delà, 409.")
    public void archiver(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl qui) {
        eleveService.archiver(id, qui);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer des élèves depuis un CSV",
            description = "Fichier CSV UTF-8 dans le champ multipart `file`, séparateur point-virgule, 2 Mo et "
                    + "1 000 lignes au maximum. Colonnes obligatoires : prenom, nom, sexe, date_naissance (JJ/MM/AAAA), "
                    + "commune_naissance, code_ecole, classe, nom_mere ; facultatives : date_naissance_presumee "
                    + "(oui/non), nom_pere, telephone_parent (+221 et 9 chiffres), langue_contact, numero_ine. "
                    + "Chaque ligne valide crée un élève et son dossier DETECTE ; le rapport donne le motif de "
                    + "chaque ligne rejetée.")
    public RapportImport importerEleves(@RequestParam("file") MultipartFile file,
                                        @AuthenticationPrincipal UserDetailsImpl qui) {
        return importCsvService.importer(file, qui);
    }
}
