package sn.samatur.samatur.statistiques.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.statistiques.dto.IndicateursDto;
import sn.samatur.samatur.statistiques.repository.IndicateurRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/** Les trois indicateurs de la fiche projet, calculés à la volée, globalement ou par commune de naissance. */
@Service
public class StatistiquesService {

    private final IndicateurRepository indicateurs;
    private final Clock horloge;
    private final int delaiCible;

    public StatistiquesService(IndicateurRepository indicateurs, Clock horloge,
                               @Value("${samatur.delai-cible-jours:15}") int delaiCible) {
        this.indicateurs = indicateurs;
        this.horloge = horloge;
        this.delaiCible = delaiCible;
    }

    /**
     * @param communeId     commune de naissance, ou null pour l'ensemble
     * @param dateReference date à laquelle I3 compte les élèves bloqués ; aujourd'hui si null
     */
    @Transactional(readOnly = true)
    public IndicateursDto calculer(Long communeId, LocalDate dateReference) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        LocalDate reference = dateReference != null ? dateReference : aujourdhui;

        // Les six étapes, toujours présentes et dans l'ordre du cycle, même à zéro
        Map<StatutDossier, Long> parEtape = new EnumMap<>(StatutDossier.class);
        for (StatutDossier etape : StatutDossier.values()) {
            parEtape.put(etape, 0L);
        }
        for (Object[] ligne : indicateurs.dossiersParEtape(communeId)) {
            parEtape.put(StatutDossier.valueOf((String) ligne[0]), ((Number) ligne[1]).longValue());
        }
        long total = parEtape.values().stream().mapToLong(Long::longValue).sum();

        return new IndicateursDto(communeId, reference, delaiCible,
                indicateurs.partTranscritsDansDelai(delaiCible, communeId),
                indicateurs.delaiMedianJours(communeId),
                indicateurs.elevesBloques(reference, communeId),
                parEtape,
                indicateurs.dossiersEnRetard(aujourdhui.minusDays(delaiCible), communeId),
                total);
    }
}
