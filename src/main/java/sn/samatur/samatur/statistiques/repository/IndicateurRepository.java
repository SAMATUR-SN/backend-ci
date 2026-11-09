package sn.samatur.samatur.statistiques.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import sn.samatur.samatur.dossier.entity.Dossier;

import java.time.LocalDate;
import java.util.List;

/**
 * Les indicateurs de la fiche projet, calculés par PostgreSQL (médiane, FILTER, différence de dates).
 * Le filtre de commune porte sur la commune de naissance de l'élève ; absent, il ne filtre rien.
 * CAST(:communeId AS BIGINT) n'est pas décoratif : sans lui, PostgreSQL refuse un paramètre null
 * dont il ne connaît pas le type.
 */
public interface IndicateurRepository extends Repository<Dossier, Long> {

    /** I1 : part des jugements transcrits dans le délai cible ; null si aucun dossier n'est transcrit. */
    @Query(value = """
            SELECT CAST(count(*) FILTER (WHERE d.date_transcription - d.date_jugement <= :delai) AS double precision)
                   / NULLIF(count(*), 0)
            FROM dossier d JOIN eleve e ON e.id = d.eleve_id AND e.archive_le IS NULL
            WHERE d.date_transcription IS NOT NULL
              AND (CAST(:communeId AS BIGINT) IS NULL OR e.commune_naissance_id = :communeId)
            """, nativeQuery = true)
    Double partTranscritsDansDelai(@Param("delai") int delai, @Param("communeId") Long communeId);

    /** I2 : délai médian, en jours, entre détection et acte remis ; null si aucun acte n'est remis. */
    @Query(value = """
            SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY d.date_remise - d.date_detection)
            FROM dossier d JOIN eleve e ON e.id = d.eleve_id AND e.archive_le IS NULL
            WHERE d.etape = 'ACTE_REMIS'
              AND (CAST(:communeId AS BIGINT) IS NULL OR e.commune_naissance_id = :communeId)
            """, nativeQuery = true)
    Double delaiMedianJours(@Param("communeId") Long communeId);

    /** I3 : élèves de CM2 dont l'acte n'est pas remis à la date de référence (RG-12). */
    @Query(value = """
            SELECT count(*)
            FROM dossier d JOIN eleve e ON e.id = d.eleve_id AND e.archive_le IS NULL
            WHERE e.classe = 'CM2'
              AND (d.date_remise IS NULL OR d.date_remise > :reference)
              AND (CAST(:communeId AS BIGINT) IS NULL OR e.commune_naissance_id = :communeId)
            """, nativeQuery = true)
    long elevesBloques(@Param("reference") LocalDate reference, @Param("communeId") Long communeId);

    /** Nombre de dossiers par étape : [etape, nombre]. */
    @Query(value = """
            SELECT d.etape, count(*)
            FROM dossier d JOIN eleve e ON e.id = d.eleve_id AND e.archive_le IS NULL
            WHERE (CAST(:communeId AS BIGINT) IS NULL OR e.commune_naissance_id = :communeId)
            GROUP BY d.etape
            """, nativeQuery = true)
    List<Object[]> dossiersParEtape(@Param("communeId") Long communeId);

    /** RG-05 : dossiers à JUGEMENT_RENDU jugés avant la limite (aujourd'hui moins le délai cible). */
    @Query(value = """
            SELECT count(*)
            FROM dossier d JOIN eleve e ON e.id = d.eleve_id AND e.archive_le IS NULL
            WHERE d.etape = 'JUGEMENT_RENDU' AND d.date_jugement < :limite
              AND (CAST(:communeId AS BIGINT) IS NULL OR e.commune_naissance_id = :communeId)
            """, nativeQuery = true)
    long dossiersEnRetard(@Param("limite") LocalDate limite, @Param("communeId") Long communeId);
}
