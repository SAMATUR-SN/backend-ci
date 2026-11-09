package sn.samatur.samatur.dossier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.HistoriqueEtape;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.repository.DossierRepository;
import sn.samatur.samatur.dossier.repository.HistoriqueEtapeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DossierService {

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private HistoriqueEtapeRepository historiqueEtapeRepository;

    // 1. Lister tous les dossiers
    public List<Dossier> listerTous() {
        return dossierRepository.findAll();
    }

    // 2. Trouver un dossier par son ID
    public Optional<Dossier> trouverParId(Long id) {
        return dossierRepository.findById(id);
    }

    // 3. Faire avancer un dossier à l'étape suivante (Machine à états)
    @Transactional
    public Dossier avancerEtape(Long dossierId, String auteur) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier avec l'ID " + dossierId + " introuvable."));

        StatutDossier ancienneEtape = dossier.getEtape();
        StatutDossier nouvelleEtape;

        // Définition de la logique séquentielle de la machine à états
        switch (ancienneEtape) {
            case DETECTE:
                nouvelleEtape = StatutDossier.CERTIFICAT_OBTENU;
                break;
            case CERTIFICAT_OBTENU:
                nouvelleEtape = StatutDossier.DOSSIER_COMPLET;
                break;
            case DOSSIER_COMPLET:
                nouvelleEtape = StatutDossier.JUGEMENT_RENDU;
                break;
            case JUGEMENT_RENDU:
                nouvelleEtape = StatutDossier.TRANSCRIT;
                break;
            case TRANSCRIT:
                nouvelleEtape = StatutDossier.ACTE_REMIS;
                break;
            case ACTE_REMIS:
                throw new IllegalStateException("Le dossier est déjà à l'étape finale (ACTE_REMIS).");
            default:
                throw new IllegalStateException("Étape actuelle inconnue.");
        }

        // Mise à jour de l'étape du dossier
        dossier.setEtape(nouvelleEtape);
        Dossier dossierSauvegarde = dossierRepository.save(dossier);

        // Enregistrement de la transition dans l'historique
        HistoriqueEtape historique = new HistoriqueEtape();
        historique.setDossier(dossierSauvegarde);
        historique.setEtapeDepart(ancienneEtape);
        historique.setEtapeArrivee(nouvelleEtape);
        historique.setDateHeure(LocalDateTime.now());
        historique.setCommentaire("Transition de " + ancienneEtape + " vers " + nouvelleEtape);
        
        historiqueEtapeRepository.save(historique);

        return dossierSauvegarde;
    }
}