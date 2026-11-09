package sn.samatur.samatur.dossier.dto;

import sn.samatur.samatur.dossier.entity.PreuveNaissance;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.entity.TypeAudience;
import sn.samatur.samatur.eleve.dto.EleveDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Le dossier complet (F3) : élève, étape courante, données de chaque étape, témoins et
 * historique horodaté. Les noms des champs d'étape sont ceux d'AvancerDossierRequest.
 * enRetard est calculé à la lecture (RG-05), jamais stocké.
 */
public record DossierDetailDto(
        Long id,
        String reference,
        StatutDossier etape,
        boolean enRetard,
        Integer joursDepuisJugement,
        EleveDto eleve,
        LocalDate dateDetection,
        String numeroCertificat,
        LocalDate dateCertificat,
        Long centreEtatCivilId,
        String centreEtatCivil,
        PreuveNaissance preuveNaissance,
        List<TemoinDto> temoins,
        String numeroJugement,
        LocalDate dateJugement,
        Long tribunalId,
        String tribunal,
        TypeAudience typeAudience,
        String numeroActe,
        Integer anneeActe,
        LocalDate dateTranscription,
        LocalDate dateRemise,
        List<HistoriqueDto> historique) {
}
