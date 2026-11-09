package sn.samatur.samatur.eleve.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sn.samatur.samatur.commun.RegleMetierException;
import sn.samatur.samatur.dossier.service.DossierService;
import sn.samatur.samatur.eleve.dto.RapportImport;
import sn.samatur.samatur.eleve.dto.RapportImport.ErreurLigne;
import sn.samatur.samatur.eleve.entity.Classe;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.eleve.entity.Langue;
import sn.samatur.samatur.eleve.repository.EleveRepository;
import sn.samatur.samatur.referentiel.entity.Commune;
import sn.samatur.samatur.referentiel.entity.Ecole;
import sn.samatur.samatur.referentiel.repository.CommuneRepository;
import sn.samatur.samatur.referentiel.repository.EcoleRepository;
import sn.samatur.samatur.security.UserDetailsImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Import d'une liste d'élèves détectés (F1). Fichier UTF-8, séparateur point-virgule, une ligne
 * d'en-tête qui nomme les colonnes (cahier des charges, section 6). L'import est ligne à ligne :
 * une ligne invalide est rejetée avec son numéro et son motif, les autres créent un élève et son
 * dossier à l'étape DETECTE.
 */
@Service
public class ImportCsvService {

    static final int LIGNES_MAX = 1000;
    /** Marque d'ordre des octets qu'Excel place en tÃªte des fichiers Â« CSV UTF-8 Â». */
    private static final String BOM = String.valueOf((char) 0xFEFF);
    /** CaractÃ¨re de remplacement : le fichier contenait des octets qui ne sont pas de l'UTF-8. */
    private static final char CARACTERE_ILLISIBLE = (char) 0xFFFD;
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final List<String> OBLIGATOIRES = List.of(
            "prenom", "nom", "sexe", "date_naissance", "commune_naissance", "code_ecole", "classe", "nom_mere");
    private static final Pattern TELEPHONE = Pattern.compile("^\\+221\\d{9}$");
    private static final String CLASSES = Arrays.stream(Classe.values()).map(Enum::name)
            .collect(Collectors.joining(", "));

    private final EleveRepository eleves;
    private final CommuneRepository communes;
    private final EcoleRepository ecoles;
    private final DossierService dossiers;
    private final Clock horloge;

    public ImportCsvService(EleveRepository eleves, CommuneRepository communes, EcoleRepository ecoles,
                            DossierService dossiers, Clock horloge) {
        this.eleves = eleves;
        this.communes = communes;
        this.ecoles = ecoles;
        this.dossiers = dossiers;
        this.horloge = horloge;
    }

    @Transactional
    public RapportImport importer(MultipartFile fichier, UserDetailsImpl qui) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier CSV absent ou vide.");
        }
        List<String> lignes = lire(fichier);
        Map<String, Integer> colonnes = lireEntete(lignes.get(0));
        if (lignes.stream().skip(1).filter(l -> !l.isBlank()).count() > LIGNES_MAX) {
            throw new RegleMetierException("fichier-trop-long",
                    "Le fichier dépasse " + LIGNES_MAX + " lignes d'élèves.", List.of());
        }

        // Les référentiels sont petits : chargés une fois plutôt qu'une requête par ligne
        Map<String, Commune> parCommune = communes.findAll().stream()
                .collect(Collectors.toMap(c -> cle(c.getNom()), Function.identity(), (a, b) -> a));
        Map<String, Ecole> parEcole = ecoles.findAll().stream()
                .collect(Collectors.toMap(e -> cle(e.getCodeEcole()), Function.identity(), (a, b) -> a));

        List<ErreurLigne> erreurs = new ArrayList<>();
        int total = 0;
        int importes = 0;
        for (int i = 1; i < lignes.size(); i++) {
            if (lignes.get(i).isBlank()) {
                continue;
            }
            total++;
            try {
                Ligne ligne = new Ligne(lignes.get(i).split(";", -1), colonnes);
                Eleve eleve = lireEleve(ligne, parCommune, parEcole);
                refuserLesDoublons(eleve);
                eleves.save(eleve);
                dossiers.creerDossierDetecte(eleve, qui, "Dossier créé par import CSV");
                importes++;
            } catch (LigneInvalideException e) {
                // Numéro de ligne du fichier, en-tête compris, comme dans Excel
                erreurs.add(new ErreurLigne(i + 1, e.getMessage()));
            }
        }
        return new RapportImport(total, importes, erreurs.size(), erreurs);
    }

    private static List<String> lire(MultipartFile fichier) {
        List<String> lignes = new ArrayList<>();
        try (BufferedReader lecteur = new BufferedReader(
                new InputStreamReader(fichier.getInputStream(), StandardCharsets.UTF_8))) {
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                lignes.add(ligne);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier CSV illisible.");
        }
        if (lignes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier CSV absent ou vide.");
        }
        // Excel ajoute un BOM en tête des fichiers « CSV UTF-8 » : sans ce retrait, la première colonne
        // ne serait pas reconnue
        if (lignes.get(0).startsWith(BOM)) {
            lignes.set(0, lignes.get(0).substring(1));
        }
        // L'ancien format « CSV » d'Excel n'est pas en UTF-8 : ses accents deviennent illisibles
        if (lignes.stream().anyMatch(l -> l.indexOf(CARACTERE_ILLISIBLE) >= 0)) {
            throw new RegleMetierException("encodage", "Le fichier n'est pas en UTF-8 : l'enregistrer depuis Excel "
                    + "au format « CSV UTF-8 (délimité par des points-virgules) ».", List.of());
        }
        return lignes;
    }

    /** Colonne → position, d'après l'en-tête ; 422 si une colonne obligatoire manque. */
    private static Map<String, Integer> lireEntete(String entete) {
        Map<String, Integer> colonnes = new HashMap<>();
        String[] noms = entete.split(";", -1);
        for (int i = 0; i < noms.length; i++) {
            colonnes.putIfAbsent(noms[i].trim().toLowerCase(Locale.ROOT), i);
        }
        List<String> manquantes = OBLIGATOIRES.stream().filter(c -> !colonnes.containsKey(c)).toList();
        if (!manquantes.isEmpty()) {
            throw new RegleMetierException("colonnes-manquantes",
                    "Colonnes obligatoires absentes de l'en-tête : " + String.join(", ", manquantes) + ".",
                    manquantes);
        }
        return colonnes;
    }

    private Eleve lireEleve(Ligne l, Map<String, Commune> parCommune, Map<String, Ecole> parEcole) {
        Eleve e = new Eleve();
        e.setPrenom(l.texte("prenom", 150, true));
        e.setNom(l.texte("nom", 100, true));

        String sexe = l.texte("sexe", 1, true).toUpperCase(Locale.ROOT);
        if (!sexe.equals("F") && !sexe.equals("M")) {
            throw new LigneInvalideException("sexe invalide : " + sexe + " (F ou M attendu)");
        }
        e.setSexe(sexe);

        String date = l.texte("date_naissance", 10, true);
        LocalDate naissance;
        try {
            naissance = LocalDate.parse(date, FORMAT_DATE);
        } catch (DateTimeParseException ex) {
            throw new LigneInvalideException("date de naissance invalide : " + date + " (JJ/MM/AAAA attendu)");
        }
        if (!naissance.isBefore(LocalDate.now(horloge))) {
            throw new LigneInvalideException("date de naissance dans le futur : " + date);
        }
        e.setDateNaissance(naissance);

        String presumee = l.texte("date_naissance_presumee", 3, false).toLowerCase(Locale.ROOT);
        if (!presumee.isEmpty() && !presumee.equals("oui") && !presumee.equals("non")) {
            throw new LigneInvalideException("date_naissance_presumee invalide : " + presumee + " (oui ou non attendu)");
        }
        e.setDateNaissancePresumee(presumee.equals("oui"));

        String commune = l.texte("commune_naissance", 150, true);
        Commune c = parCommune.get(cle(commune));
        if (c == null) {
            throw new LigneInvalideException("commune inconnue : " + commune);
        }
        e.setCommuneNaissance(c);

        String code = l.texte("code_ecole", 50, true);
        Ecole ecole = parEcole.get(cle(code));
        if (ecole == null) {
            throw new LigneInvalideException("école inconnue : " + code);
        }
        e.setEcole(ecole);

        String classe = l.texte("classe", 20, true).toUpperCase(Locale.ROOT);
        try {
            e.setClasse(Classe.valueOf(classe).name());
        } catch (IllegalArgumentException ex) {
            throw new LigneInvalideException("classe invalide : " + classe + " (" + CLASSES + ")");
        }

        e.setNomMere(l.texte("nom_mere", 150, true));
        e.setNomPere(videEnNull(l.texte("nom_pere", 150, false)));

        String telephone = l.texte("telephone_parent", 20, false).replace(" ", "");
        if (!telephone.isEmpty() && !TELEPHONE.matcher(telephone).matches()) {
            throw new LigneInvalideException("téléphone invalide : +221 suivi de 9 chiffres attendu");
        }
        e.setTelephoneParent(videEnNull(telephone));

        String langue = l.texte("langue_contact", 20, false).toUpperCase(Locale.ROOT);
        try {
            e.setLangueContact(langue.isEmpty() ? Langue.FRANCAIS : Langue.valueOf(langue));
        } catch (IllegalArgumentException ex) {
            throw new LigneInvalideException("langue de contact invalide : " + langue
                    + " (FRANCAIS, WOLOF, PULAAR ou SEREER)");
        }

        e.setNumeroIne(videEnNull(l.texte("numero_ine", 50, false)));
        return e;
    }

    /** RG-07, et unicité du numéro INE. Les lignes déjà importées du même fichier sont prises en compte. */
    private void refuserLesDoublons(Eleve e) {
        if (eleves.existsByPrenomIgnoreCaseAndNomIgnoreCaseAndDateNaissanceAndEcoleIdAndArchiveLeIsNull(
                e.getPrenom(), e.getNom(), e.getDateNaissance(), e.getEcole().getId())) {
            throw new LigneInvalideException("doublon : un élève de même prénom, nom, date de naissance "
                    + "et école existe déjà");
        }
        if (e.getNumeroIne() != null && eleves.existsByNumeroIne(e.getNumeroIne())) {
            throw new LigneInvalideException("numéro INE déjà utilisé : " + e.getNumeroIne());
        }
    }

    private static String cle(String valeur) {
        return valeur == null ? "" : valeur.trim().toLowerCase(Locale.ROOT);
    }

    private static String videEnNull(String valeur) {
        return valeur.isEmpty() ? null : valeur;
    }

    /** Une ligne découpée, lue par nom de colonne. */
    private static final class Ligne {
        private final String[] valeurs;
        private final Map<String, Integer> colonnes;

        Ligne(String[] valeurs, Map<String, Integer> colonnes) {
            int attendues = colonnes.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
            if (valeurs.length > attendues) {
                throw new LigneInvalideException("trop de colonnes : " + valeurs.length + " au lieu de " + attendues
                        + " (un point-virgule dans un champ ?)");
            }
            this.valeurs = valeurs;
            this.colonnes = colonnes;
        }

        /** Valeur sans espaces autour ; vide si la colonne est absente ou la ligne plus courte. */
        String texte(String colonne, int longueurMax, boolean obligatoire) {
            Integer i = colonnes.get(colonne);
            String v = i == null || i >= valeurs.length ? "" : valeurs[i].trim();
            if (obligatoire && v.isEmpty()) {
                throw new LigneInvalideException("champ obligatoire manquant : " + colonne);
            }
            if (v.length() > longueurMax) {
                throw new LigneInvalideException(colonne + " trop long (" + longueurMax + " caractères au maximum)");
            }
            return v;
        }
    }

    /** Ligne rejetée : son motif rejoint le rapport, les autres lignes continuent. */
    private static final class LigneInvalideException extends RuntimeException {
        LigneInvalideException(String motif) {
            super(motif);
        }
    }
}
