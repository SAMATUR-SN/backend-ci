package sn.samatur.samatur.alerte.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sn.samatur.samatur.alerte.entity.Alerte;
import sn.samatur.samatur.alerte.repository.AlerteRepository;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.repository.DossierRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AlerteService {

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private AlerteRepository alerteRepository;

    // Vérifie tous les dossiers et génère des alertes si les délais sont dépassés
    public void verifierRetardsDossiers() {
        List<Dossier> dossiers = dossierRepository.findAll();
        LocalDate aujourdHui = LocalDate.now();

        for (Dossier dossier : dossiers) {
            // Ignorer les dossiers déjà terminés
            if (dossier.getEtape() == StatutDossier.REMIS) {
                continue;
            }

            long joursEcoules = 0;
            // On prend comme référence la date de detection ou une date d'étape selon le cas
            if (dossier.getDateDetection() != null) {
                joursEcoules = ChronoUnit.DAYS.between(dossier.getDateDetection(), aujourdHui);
            }

            int seuilLimite = obtenirSeuilParEtape(dossier.getEtape());

            if (joursEcoules > seuilLimite) {
                // Vérifier si une alerte non résolue existe déjà pour ce dossier
                boolean alerteExiste = alerteRepository.existsByDossierIdAndResolueFalse(dossier.getId());
                
                if (!alerteExiste) {
                    Alerte alerte = new Alerte();
                    alerte.setDossier(dossier);
                    alerte.setMessage("Le dossier " + dossier.getReference() + " est bloqué à l'étape " + dossier.getEtape() + " depuis " + joursEcoules + " jours (seuil : " + seuilLimite + " jours).");
                    alerte.setDateAlerte(LocalDateTime.now());
                    alerte.setResolue(false);
                    
                    alerteRepository.save(alerte);
                }
            }
        }
    }

    // Définition des seuils en jours par étape
    private int obtenirSeuilParEtape(StatutDossier etape) {
        switch (etape) {
            case DETECTE:
                return 15;
            case CERTIFICAT_ETABLI:
                return 30;
            case DOSSIER_COMPLET:
                return 20;
            case JUGEMENT_RENDU:
            case TRANSCRIT:
                return 15;
            default:
                return 30;
        }
    }
}