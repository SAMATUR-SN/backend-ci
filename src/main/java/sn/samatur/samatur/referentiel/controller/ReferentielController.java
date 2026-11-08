package sn.samatur.samatur.referentiel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.samatur.samatur.referentiel.entity.Commune;
import sn.samatur.samatur.referentiel.entity.Ecole;
import sn.samatur.samatur.referentiel.repository.CommuneRepository;
import sn.samatur.samatur.referentiel.repository.EcoleRepository;

import java.util.List;

@RestController
@RequestMapping("/api/referentiels")
@Tag(name = "Référentiels")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL', 'SUPERVISEUR')")
public class ReferentielController {

    private final EcoleRepository ecoleRepository;
    private final CommuneRepository communeRepository;

    public ReferentielController(EcoleRepository ecoleRepository, CommuneRepository communeRepository) {
        this.ecoleRepository = ecoleRepository;
        this.communeRepository = communeRepository;
    }

    @GetMapping("/ecoles")
    @Operation(summary = "Lister les écoles")
    public ResponseEntity<List<Ecole>> listerEcoles() {
        return ResponseEntity.ok(ecoleRepository.findAll());
    }

    @GetMapping("/communes")
    @Operation(summary = "Lister les communes")
    public ResponseEntity<List<Commune>> listerCommunes() {
        return ResponseEntity.ok(communeRepository.findAll());
    }
}