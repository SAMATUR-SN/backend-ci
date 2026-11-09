package sn.samatur.samatur.dossier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.samatur.samatur.dossier.entity.PreuveNaissance;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.entity.TypeAudience;

import java.time.LocalDate;
import java.util.List;

/**
 * Une seule requête pour toutes les étapes. etapeCible est obligatoire et doit être l'étape
 * suivant l'étape courante : un rejeu est refusé (409) au lieu de faire avancer deux fois.
 * Les autres champs sont facultatifs ici ; le service exige ceux de l'étape visée
 * (cahier des charges, section 3).
 */
public record AvancerDossierRequest(
        @NotNull StatutDossier etapeCible,
        // CERTIFICAT_OBTENU (ETAT_CIVIL)
        @Size(max = 100) String numeroCertificat, LocalDate dateCertificat, Long centreEtatCivilId,
        // DOSSIER_COMPLET (ECOLE) : deux témoins si la preuve est TEMOINS
        PreuveNaissance preuveNaissance, List<@Valid TemoinDto> temoins,
        // JUGEMENT_RENDU (TRIBUNAL)
        @Size(max = 100) String numeroJugement, LocalDate dateJugement, Long tribunalId, TypeAudience typeAudience,
        // TRANSCRIT (ETAT_CIVIL)
        @Size(max = 100) String numeroActe, Integer anneeActe, LocalDate dateTranscription,
        // ACTE_REMIS (ECOLE)
        LocalDate dateRemise,
        // Facultatif, enregistré dans l'historique
        @Size(max = 500) String commentaire) {
}
