package sn.samatur.samatur.statistiques.dto;

import sn.samatur.samatur.dossier.entity.StatutDossier;

import java.time.LocalDate;
import java.util.Map;

/**
 * Le tableau de bord (F6). I1 est une part entre 0 et 1, I2 un nombre de jours ; tous deux valent
 * null quand aucun dossier n'est concerné (pas 0). I3 vaut 0 dans ce cas.
 */
public record IndicateursDto(
        Long communeId,
        LocalDate dateReference,
        int delaiCibleJours,
        /* I1 : part des jugements transcrits dans le délai cible */
        Double i1PartTranscritsDansDelaiCible,
        /* I2 : délai médian, en jours, entre détection et acte remis */
        Double i2DelaiMedianJours,
        /* I3 : élèves de CM2 sans acte remis à la date de référence */
        long i3ElevesClasseExamenSansActe,
        Map<StatutDossier, Long> dossiersParEtape,
        long dossiersEnRetard,
        long totalDossiers) {
}
