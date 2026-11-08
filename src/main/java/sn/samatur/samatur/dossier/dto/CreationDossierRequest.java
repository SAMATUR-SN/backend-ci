package sn.samatur.samatur.dossier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreationDossierRequest(
        @NotBlank String reference,
        @NotNull Long eleveId) {
}
