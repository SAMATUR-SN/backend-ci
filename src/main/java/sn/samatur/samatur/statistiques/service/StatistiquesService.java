package sn.samatur.samatur.statistiques.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sn.samatur.samatur.alerte.repository.AlerteRepository;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.repository.DossierRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatistiquesService {

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private AlerteRepository alerteRepository;

    public Map<String, Object> obtenirTableauDeBord() {
        Map<String, Object> stats = new HashMap<>();

        long totalDossiers = dossierRepository.count();
        stats.put("totalDossiers", totalDossiers);

        // Répartition par étape
        Map<String, Long> parEtape = new HashMap<>();
        for (StatutDossier statut : StatutDossier.values()) {
            parEtape.put(statut.name(), dossierRepository.countByEtape(statut));
        }
        stats.put("repartitionParEtape", parEtape);

        // Taux d'achèvement (Dossiers à l'étape ACTE_REMIS / Total)
        long dossiersTermines = dossierRepository.countByEtape(StatutDossier.ACTE_REMIS);
        double tauxAchèvement = totalDossiers > 0 ? ((double) dossiersTermines / totalDossiers) * 100 : 0.0;
        stats.put("tauxAchèvementPourcent", tauxAchèvement);

        // Nombre d'alertes actives
        long totalAlertesActives = alerteRepository.findByResolueFalse().size();
        stats.put("totalAlertesActives", totalAlertesActives);

        // Répartition par école
        List<Object[]> parEcole = dossierRepository.countDossiersParEcole();
        stats.put("dossiersParEcole", parEcole);

        // Répartition par centre d'état civil
        List<Object[]> parCentre = dossierRepository.countDossiersParCentreEtatCivil();
        stats.put("dossiersParCentreEtatCivil", parCentre);

        return stats;
    }
}