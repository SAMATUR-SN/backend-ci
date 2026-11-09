package sn.samatur.samatur.dossier.dto;

import jakarta.validation.constraints.Size;
import sn.samatur.samatur.dossier.entity.Temoin;

/** Témoin de la naissance : nom complet et numéro de pièce d'identité. */
public record TemoinDto(@Size(max = 200) String nomComplet, @Size(max = 100) String numeroPiece) {

    public static TemoinDto de(Temoin t) {
        return new TemoinDto(t.getNomComplet(), t.getNumeroPiece());
    }
}
