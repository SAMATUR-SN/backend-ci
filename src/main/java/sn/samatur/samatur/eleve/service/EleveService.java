package sn.samatur.samatur.eleve.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.eleve.repository.EleveRepository;
import sn.samatur.samatur.referentiel.entity.Ecole;
import sn.samatur.samatur.referentiel.repository.EcoleRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EleveService {

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private EcoleRepository ecoleRepository;

    // --- 1. CRUD : Lister tous les élèves ---
    public List<Eleve> listerTous() {
        return eleveRepository.findAll();
    }

    // --- 2. CRUD : Trouver un élève par son ID ---
    public Optional<Eleve> trouverParId(Long id) {
        return eleveRepository.findById(id);
    }

    // --- 3. CRUD : Enregistrer ou Créer un élève (unitaire) ---
    public Eleve enregistrerEleve(Eleve eleve) {
        if (eleve.getDateNaissancePresumee() == null) {
            eleve.setDateNaissancePresumee(false);
        }
        return eleveRepository.save(eleve);
    }

    // --- 4. CRUD : Modifier un élève existant ---
    public Eleve modifierEleve(Long id, Eleve eleveDetails) {
        Eleve eleve = eleveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Élève avec l'ID " + id + " introuvable."));

        eleve.setNumeroIne(eleveDetails.getNumeroIne());
        eleve.setNom(eleveDetails.getNom());
        eleve.setPrenom(eleveDetails.getPrenom());
        eleve.setClasse(eleveDetails.getClasse());
        eleve.setSexe(eleveDetails.getSexe());
        eleve.setDateNaissance(eleveDetails.getDateNaissance());
        
        if (eleveDetails.getDateNaissancePresumee() != null) {
            eleve.setDateNaissancePresumee(eleveDetails.getDateNaissancePresumee());
        }
        
        eleve.setEcole(eleveDetails.getEcole());
        eleve.setCommuneNaissance(eleveDetails.getCommuneNaissance());
        eleve.setNomPere(eleveDetails.getNomPere());
        eleve.setProfessionPere(eleveDetails.getProfessionPere());
        eleve.setNomMere(eleveDetails.getNomMere());
        eleve.setProfessionMere(eleveDetails.getProfessionMere());

        return eleveRepository.save(eleve);
    }

    // --- 5. CRUD : Supprimer un élève ---
    public void supprimerEleve(Long id) {
        if (!eleveRepository.existsById(id)) {
            throw new RuntimeException("Élève avec l'ID " + id + " introuvable.");
        }
        eleveRepository.deleteById(id);
    }

    // --- 6. Importation CSV (Fonctionnelle) ---
    public List<String> importerCsv(MultipartFile file) {
        List<String> erreurs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String ligne;
            int numeroLigne = 0;
            
            // Ignorer l'en-tête du CSV si présent
            ligne = br.readLine();
            if (ligne != null && ligne.toLowerCase().contains("ine")) {
                numeroLigne++;
            }

            while ((ligne = br.readLine()) != null) {
                numeroLigne++;
                String[] colonnes = ligne.split(";"); // Séparateur point-virgule

                // On s'attend au moins à 7 colonnes : numero_ine, nom, prenom, classe, ecole_id, date_naissance, sexe
                if (colonnes.length < 7) {
                    erreurs.add("Ligne " + numeroLigne + " : Format invalide (colonnes insuffisantes, 7 attendues).");
                    continue;
                }

                try {
                    String numeroIne = colonnes[0].trim();
                    String nom = colonnes[1].trim();
                    String prenom = colonnes[2].trim();
                    String classe = colonnes[3].trim();
                    Long ecoleId = Long.parseLong(colonnes[4].trim());
                    String dateNaissanceStr = colonnes[5].trim();
                    String sexe = colonnes[6].trim();

                    Ecole ecole = ecoleRepository.findById(ecoleId)
                            .orElseThrow(() -> new RuntimeException("École ID " + ecoleId + " introuvable"));

                    Eleve eleve = eleveRepository.findByNumeroIne(numeroIne).orElse(new Eleve());
                    eleve.setNumeroIne(numeroIne);
                    eleve.setNom(nom);
                    eleve.setPrenom(prenom);
                    eleve.setClasse(classe);
                    eleve.setEcole(ecole);
                    eleve.setSexe(sexe);

                    // Valeur par défaut obligatoire pour éviter l'erreur NOT NULL en base
                    eleve.setDateNaissancePresumee(false); 

                    if (!dateNaissanceStr.isEmpty()) {
                        eleve.setDateNaissance(LocalDate.parse(dateNaissanceStr));
                    }

                    // Si ton CSV fournit la 8ème colonne pour date_naissance_presumee (true/false)
                    if (colonnes.length > 7 && !colonnes[7].trim().isEmpty()) {
                        eleve.setDateNaissancePresumee(Boolean.parseBoolean(colonnes[7].trim()));
                    }

                    eleveRepository.save(eleve);
                } catch (Exception e) {
                    erreurs.add("Ligne " + numeroLigne + " : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            erreurs.add("Erreur globale lors de la lecture du fichier : " + e.getMessage());
        }
        return erreurs;
    }
}