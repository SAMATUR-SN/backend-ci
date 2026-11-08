package sn.samatur.samatur.eleve.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.eleve.repository.EleveRepository;
import sn.samatur.samatur.referentiel.entity.Commune;
import sn.samatur.samatur.referentiel.entity.Ecole;
import sn.samatur.samatur.referentiel.repository.CommuneRepository;
import sn.samatur.samatur.referentiel.repository.EcoleRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class EleveService {

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private EcoleRepository ecoleRepository;

    @Autowired
    private CommuneRepository communeRepository;

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
        verifierEtChargerReferentiels(eleve);
        if (eleve.getDateNaissancePresumee() == null) {
            eleve.setDateNaissancePresumee(false);
        }
        return eleveRepository.save(eleve);
    }

    // --- 4. CRUD : Modifier un élève existant ---
    public Eleve modifierEleve(Long id, Eleve eleveDetails) {
        Eleve eleve = eleveRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Élève avec l'ID " + id + " introuvable."));

        eleve.setNumeroIne(eleveDetails.getNumeroIne());
        eleve.setNom(eleveDetails.getNom());
        eleve.setPrenom(eleveDetails.getPrenom());
        eleve.setClasse(eleveDetails.getClasse());
        eleve.setSexe(eleveDetails.getSexe());
        eleve.setDateNaissance(eleveDetails.getDateNaissance());
        
        if (eleveDetails.getDateNaissancePresumee() != null) {
            eleve.setDateNaissancePresumee(eleveDetails.getDateNaissancePresumee());
        }
        
        verifierEtChargerReferentiels(eleveDetails);
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
            throw new NoSuchElementException("Élève avec l'ID " + id + " introuvable.");
        }
        eleveRepository.deleteById(id);
    }

    // --- 6. Importation CSV (Fonctionnelle) ---
    public List<String> importerCsv(MultipartFile file) {
        List<String> erreurs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String ligne;
            int numeroLigne = 0;

            while ((ligne = br.readLine()) != null) {
                numeroLigne++;
                if (numeroLigne == 1 && ligne.toLowerCase().contains("ine")) {
                    continue;
                }
                String[] colonnes = ligne.split(";", -1);

                // Required columns: INE, name, first name, class, school ID, birth date, sex, birth commune ID.
                if (colonnes.length < 8) {
                    erreurs.add("Ligne " + numeroLigne + " : 8 colonnes obligatoires attendues, dont commune_naissance_id en 8e position.");
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
                    if (colonnes[7].trim().isEmpty()) {
                        throw new IllegalArgumentException("La commune de naissance est obligatoire.");
                    }
                    Long communeId = Long.parseLong(colonnes[7].trim());

                    if (dateNaissanceStr.isEmpty()) {
                        throw new IllegalArgumentException("La date de naissance est obligatoire.");
                    }
                    if (!"M".equals(sexe) && !"F".equals(sexe)) {
                        throw new IllegalArgumentException("Le sexe doit être M ou F.");
                    }

                    Ecole ecole = ecoleRepository.findById(ecoleId)
                            .orElseThrow(() -> new RuntimeException("École ID " + ecoleId + " introuvable"));
                    Commune commune = communeRepository.findById(communeId)
                            .orElseThrow(() -> new RuntimeException("Commune ID " + communeId + " introuvable"));

                    Eleve eleve = eleveRepository.findByNumeroIne(numeroIne).orElse(new Eleve());
                    eleve.setNumeroIne(numeroIne);
                    eleve.setNom(nom);
                    eleve.setPrenom(prenom);
                    eleve.setClasse(classe);
                    eleve.setEcole(ecole);
                    eleve.setCommuneNaissance(commune);
                    eleve.setSexe(sexe);

                    eleve.setDateNaissance(LocalDate.parse(dateNaissanceStr));
                    eleve.setDateNaissancePresumee(false);

                    if (colonnes.length > 8 && !colonnes[8].trim().isEmpty()) {
                        String datePresumee = colonnes[8].trim();
                        if (!"true".equalsIgnoreCase(datePresumee) && !"false".equalsIgnoreCase(datePresumee)) {
                            throw new IllegalArgumentException("date_naissance_presumee doit valoir true ou false.");
                        }
                        eleve.setDateNaissancePresumee(Boolean.parseBoolean(datePresumee));
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

    private void verifierEtChargerReferentiels(Eleve eleve) {
        if (eleve.getEcole() == null || eleve.getEcole().getId() == null) {
            throw new IllegalArgumentException("Une école valide est obligatoire.");
        }
        if (eleve.getCommuneNaissance() == null || eleve.getCommuneNaissance().getId() == null) {
            throw new IllegalArgumentException("Une commune de naissance valide est obligatoire.");
        }

        Ecole ecole = ecoleRepository.findById(eleve.getEcole().getId())
                .orElseThrow(() -> new IllegalArgumentException("École introuvable."));
        Commune commune = communeRepository.findById(eleve.getCommuneNaissance().getId())
                .orElseThrow(() -> new IllegalArgumentException("Commune de naissance introuvable."));
        eleve.setEcole(ecole);
        eleve.setCommuneNaissance(commune);
    }
}