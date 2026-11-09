package sn.samatur.samatur.alerte.dto;

import sn.samatur.samatur.alerte.entity.Alerte;

import java.time.LocalDateTime;

/** Une alerte de retard telle que l'API l'expose. */
public record AlerteDto(Long id, Long dossierId, String reference, String message,
                        LocalDateTime dateAlerte, boolean resolue) {

    public static AlerteDto de(Alerte a) {
        return new AlerteDto(a.getId(), a.getDossier().getId(), a.getDossier().getReference(),
                a.getMessage(), a.getDateAlerte(), a.isResolue());
    }
}
