package sn.samatur.samatur.dossier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.HistoriqueEtape;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.dto.CreationDossierRequest;
import sn.samatur.samatur.dossier.repository.DossierRepository;
import sn.samatur.samatur.dossier.repository.HistoriqueEtapeRepository;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.eleve.repository.EleveRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class DossierService {

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private HistoriqueEtapeRepository historiqueEtapeRepository;

    @Autowired
    private EleveRepository eleveRepository;

    // 1. Lister tous les dossiers
    public List<Dossier> listerTous() {
        return dossierRepository.findAll();
    }

    // 2. Trouver un dossier par son ID
    public Optional<Dossier> trouverParId(Long id) {
        return dossierRepository.findById(id);
    }

    @Transactional
    public Dossier creerDossier(CreationDossierRequest request) {
        String reference = request.reference().trim();
        if (dossierRepository.existsByReference(reference)) {
            throw new IllegalArgumentException("Cette référence de dossier existe déjà.");
        }

        Eleve eleve = eleveRepository.findById(request.eleveId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Élève avec l'ID " + request.eleveId() + " introuvable."));
        if (dossierRepository.findByEleveId(eleve.getId()).isPresent()) {
            throw new IllegalStateException("Cet élève possède déjà un dossier.");
        }

        Dossier dossier = new Dossier();
        dossier.setReference(reference);
        dossier.setEleve(eleve);
        dossier.setEtape(StatutDossier.DETECTE);
        dossier.setDateDetection(LocalDate.now());
        Dossier dossierSauvegarde = dossierRepository.save(dossier);

        enregistrerHistorique(dossierSauvegarde, null, StatutDossier.DETECTE,
                "Création du dossier");
        return dossierSauvegarde;
    }

    public List<HistoriqueEtape> obtenirHistorique(Long dossierId) {
        if (!dossierRepository.existsById(dossierId)) {
            throw new NoSuchElementException("Dossier avec l'ID " + dossierId + " introuvable.");
        }
        return historiqueEtapeRepository.findByDossierIdOrderByDateHeureAscIdAsc(dossierId);
    }

    // 3. Faire avancer un dossier à l'étape suivante (Machine à états)
    @Transactional
    public Dossier avancerEtape(Long dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new NoSuchElementException("Dossier avec l'ID " + dossierId + " introuvable."));

        StatutDossier ancienneEtape = dossier.getEtape();
        StatutDossier nouvelleEtape;

        // Définition de la logique séquentielle de la machine à états
        switch (ancienneEtape) {
            case DETECTE:
                nouvelleEtape = StatutDossier.CERTIFICAT_ETABLI;
                break;
            case CERTIFICAT_ETABLI:
                nouvelleEtape = StatutDossier.DOSSIER_COMPLET;
                break;
            case DOSSIER_COMPLET:
                nouvelleEtape = StatutDossier.JUGEMENT_RENDU;
                break;
            case JUGEMENT_RENDU:
                nouvelleEtape = StatutDossier.TRANSCRIT;
                break;
            case TRANSCRIT:
                nouvelleEtape = StatutDossier.REMIS;
                break;
            case REMIS:
                throw new IllegalStateException("Le dossier est déjà à l'étape finale (REMIS).");
            default:
                throw new IllegalStateException("Étape actuelle inconnue.");
        }

        // Mise à jour de l'étape du dossier
        dossier.setEtape(nouvelleEtape);
        Dossier dossierSauvegarde = dossierRepository.save(dossier);

        // Enregistrement de la transition dans l'historique
        enregistrerHistorique(dossierSauvegarde, ancienneEtape, nouvelleEtape,
                "Transition de " + ancienneEtape + " vers " + nouvelleEtape);

        return dossierSauvegarde;
    }

    private void enregistrerHistorique(Dossier dossier, StatutDossier etapeDepart,
            StatutDossier etapeArrivee, String commentaire) {
        HistoriqueEtape historique = new HistoriqueEtape();
        historique.setDossier(dossier);
        historique.setEtapeDepart(etapeDepart);
        historique.setEtapeArrivee(etapeArrivee);
        historique.setDateHeure(LocalDateTime.now());
        historique.setCommentaire(commentaire);
        historiqueEtapeRepository.save(historique);
    }
}