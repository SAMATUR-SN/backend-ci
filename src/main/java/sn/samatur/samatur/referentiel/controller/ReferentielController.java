package sn.samatur.samatur.referentiel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.samatur.samatur.referentiel.repository.CentreEtatCivilRepository;
import sn.samatur.samatur.referentiel.repository.CommuneRepository;
import sn.samatur.samatur.referentiel.repository.EcoleRepository;
import sn.samatur.samatur.referentiel.repository.TribunalInstanceRepository;

import java.util.List;

/** Listes des formulaires : communes, écoles, centres d'état civil et tribunaux, chargés par Flyway. */
@RestController
@RequestMapping("/api/referentiels")
@Tag(name = "Référentiels")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class ReferentielController {

    public record OptionDto(Long id, String nom) {
    }

    public record EcoleDto(Long id, String code, String nom, Long communeId) {
    }

    private final CommuneRepository communes;
    private final EcoleRepository ecoles;
    private final CentreEtatCivilRepository centres;
    private final TribunalInstanceRepository tribunaux;

    public ReferentielController(CommuneRepository communes, EcoleRepository ecoles,
                                 CentreEtatCivilRepository centres, TribunalInstanceRepository tribunaux) {
        this.communes = communes;
        this.ecoles = ecoles;
        this.centres = centres;
        this.tribunaux = tribunaux;
    }

    @GetMapping("/communes")
    @Operation(summary = "Lister les communes")
    public List<OptionDto> communes() {
        return communes.findAll(Sort.by("nom")).stream()
                .map(c -> new OptionDto(c.getId(), c.getNom())).toList();
    }

    @GetMapping("/ecoles")
    @Operation(summary = "Lister les écoles")
    @Transactional(readOnly = true)
    public List<EcoleDto> ecoles() {
        return ecoles.findAll(Sort.by("nom")).stream()
                .map(e -> new EcoleDto(e.getId(), e.getCodeEcole(), e.getNom(),
                        e.getCommune() != null ? e.getCommune().getId() : null))
                .toList();
    }

    @GetMapping("/centres")
    @Operation(summary = "Lister les centres d'état civil")
    public List<OptionDto> centres() {
        return centres.findAll(Sort.by("nom")).stream()
                .map(c -> new OptionDto(c.getId(), c.getNom())).toList();
    }

    @GetMapping("/tribunaux")
    @Operation(summary = "Lister les tribunaux d'instance")
    public List<OptionDto> tribunaux() {
        return tribunaux.findAll(Sort.by("nom")).stream()
                .map(t -> new OptionDto(t.getId(), t.getNom())).toList();
    }
}
