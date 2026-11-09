package sn.samatur.samatur.eleve.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import sn.samatur.samatur.commun.RegleMetierException;
import sn.samatur.samatur.dossier.service.DossierService;
import sn.samatur.samatur.eleve.dto.RapportImport;
import sn.samatur.samatur.eleve.dto.RapportImport.ErreurLigne;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.eleve.entity.Langue;
import sn.samatur.samatur.eleve.repository.EleveRepository;
import sn.samatur.samatur.referentiel.entity.Commune;
import sn.samatur.samatur.referentiel.entity.Ecole;
import sn.samatur.samatur.referentiel.repository.CommuneRepository;
import sn.samatur.samatur.referentiel.repository.EcoleRepository;
import sn.samatur.samatur.security.UserDetailsImpl;
import sn.samatur.samatur.security.entity.Utilisateur;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Import CSV (F1, RG-07), sans base : référentiels et dépôts simulés, « aujourd'hui » fixé au 10/11/2026. */
@ExtendWith(MockitoExtension.class)
class ImportCsvServiceTest {

    private static final String ENTETE = "prenom;nom;sexe;date_naissance;date_naissance_presumee;commune_naissance;"
            + "code_ecole;classe;nom_mere;nom_pere;telephone_parent;langue_contact;numero_ine";

    @Mock EleveRepository eleves;
    @Mock CommuneRepository communes;
    @Mock EcoleRepository ecoles;
    @Mock DossierService dossiers;

    ImportCsvService service;
    UserDetailsImpl ecole;

    @BeforeEach
    void preparer() {
        Clock horloge = Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC);
        service = new ImportCsvService(eleves, communes, ecoles, dossiers, horloge);

        Commune bambey = new Commune();
        bambey.setId(1L);
        bambey.setNom("Bambey (Commune)");
        Commune ngoye = new Commune();
        ngoye.setId(2L);
        ngoye.setNom("Ngoye");
        lenient().when(communes.findAll()).thenReturn(List.of(bambey, ngoye));

        Ecole station = new Ecole();
        station.setId(1L);
        station.setCodeEcole("ECO-BAM-01");
        lenient().when(ecoles.findAll()).thenReturn(List.of(station));

        Utilisateur u = new Utilisateur();
        u.setId(1L);
        u.setIdentifiant("ecole1@samatur.sn");
        u.setMotDePasseHash("x");
        u.setRole("ECOLE");
        u.setActif(true);
        ecole = UserDetailsImpl.build(u);
    }

    private static MockMultipartFile fichier(String contenu) {
        return fichier(contenu, StandardCharsets.UTF_8);
    }

    private static MockMultipartFile fichier(String contenu, Charset encodage) {
        return new MockMultipartFile("file", "eleves.csv", "text/csv", contenu.getBytes(encodage));
    }

    @Test // RG-07 : les lignes invalides sont rejetées avec leur motif, les autres importées
    void importe_les_lignes_valides_et_rejette_le_doublon_et_la_commune_inconnue() {
        lenient().when(eleves.existsByPrenomIgnoreCaseAndNomIgnoreCaseAndDateNaissanceAndEcoleId(
                eq("Fatou"), eq("Sarr"), any(), eq(1L))).thenReturn(true);
        String csv = String.join("\n", ENTETE,
                "Awa;Diouf;F;14/03/2014;non;Bambey (Commune);ECO-BAM-01;CM2;Mariama Sene;;;;",
                "Moussa;Ndiaye;M;02/07/2014;oui;Bambeye;ECO-BAM-01;CM2;Fatou Sarr;;;;",
                "",
                "Fatou;Sarr;F;20/09/2013;;Bambey (Commune);ECO-BAM-01;CM2;Sokhna Ndiaye;;;;",
                "Ibrahima;Sow;M;14/08/2014;;Ngoye;eco-bam-01;cm1;Dieynaba Ka;;+221 77 000 00 11;pulaar;");

        RapportImport rapport = service.importer(fichier(csv), ecole);

        assertThat(rapport.total()).isEqualTo(4);
        assertThat(rapport.importes()).isEqualTo(2);
        assertThat(rapport.rejetes()).isEqualTo(2);
        assertThat(rapport.erreurs()).containsExactly(
                new ErreurLigne(3, "commune inconnue : Bambeye"),
                new ErreurLigne(5, "doublon : un élève de même prénom, nom, date de naissance et école existe déjà"));
        verify(dossiers, times(2)).creerDossierDetecte(any(), eq(ecole), anyString());
    }

    @Test // Idée reprise du test de Gana : l'import renseigne la commune de naissance (NOT NULL en base)
    void l_import_renseigne_la_commune_de_naissance_et_les_champs_facultatifs() {
        service.importer(fichier(ENTETE + "\nIbrahima;Sow;M;14/08/2014;oui;Ngoye;ECO-BAM-01;CM1;Dieynaba Ka;Samba Sow;"
                + "+221770000011;PULAAR;INE-42"), ecole);

        ArgumentCaptor<Eleve> eleve = ArgumentCaptor.forClass(Eleve.class);
        verify(eleves).save(eleve.capture());
        assertThat(eleve.getValue().getCommuneNaissance().getId()).isEqualTo(2L);
        assertThat(eleve.getValue().getEcole().getId()).isEqualTo(1L);
        assertThat(eleve.getValue().getDateNaissance()).isEqualTo(LocalDate.of(2014, 8, 14));
        assertThat(eleve.getValue().getDateNaissancePresumee()).isTrue();
        assertThat(eleve.getValue().getClasse()).isEqualTo("CM1");
        assertThat(eleve.getValue().getTelephoneParent()).isEqualTo("+221770000011");
        assertThat(eleve.getValue().getLangueContact()).isEqualTo(Langue.PULAAR);
        assertThat(eleve.getValue().getNumeroIne()).isEqualTo("INE-42");
    }

    @Test // 6.5 : Excel ajoute un BOM en tête des fichiers « CSV UTF-8 »
    void lit_un_fichier_avec_le_bom_d_excel() {
        RapportImport rapport = service.importer(fichier(String.valueOf((char) 0xFEFF) + ENTETE
                + "\nAwa;Diouf;F;14/03/2014;non;Bambey (Commune);ECO-BAM-01;CM2;Mariama Sene;;;;"), ecole);

        assertThat(rapport.importes()).isEqualTo(1);
    }

    @Test
    void chaque_valeur_invalide_donne_un_motif_precis() {
        String csv = String.join("\n", ENTETE,
                "Awa;Diouf;X;14/03/2014;;Ngoye;ECO-BAM-01;CM2;Mariama Sene;;;;",
                "Awa;Diouf;F;2014-03-14;;Ngoye;ECO-BAM-01;CM2;Mariama Sene;;;;",
                "Awa;Diouf;F;31/02/2014;;Ngoye;ECO-BAM-01;CM2;Mariama Sene;;;;",
                "Awa;Diouf;F;14/03/2027;;Ngoye;ECO-BAM-01;CM2;Mariama Sene;;;;",
                "Awa;Diouf;F;14/03/2014;;Ngoye;ECO-XXX;CM2;Mariama Sene;;;;",
                "Awa;Diouf;F;14/03/2014;;Ngoye;ECO-BAM-01;6EME;Mariama Sene;;;;",
                "Awa;Diouf;F;14/03/2014;;Ngoye;ECO-BAM-01;CM2;;;;;",
                "Awa;Diouf;F;14/03/2014;;Ngoye;ECO-BAM-01;CM2;Mariama Sene;;770000001;;",
                "Awa;Diouf;F;14/03/2014;;Ngoye;ECO-BAM-01;CM2;Mariama Sene;;;ANGLAIS;");

        RapportImport rapport = service.importer(fichier(csv), ecole);

        assertThat(rapport.importes()).isZero();
        assertThat(rapport.erreurs()).extracting(ErreurLigne::motif).containsExactly(
                "sexe invalide : X (F ou M attendu)",
                "date de naissance invalide : 2014-03-14 (JJ/MM/AAAA attendu)",
                "date de naissance invalide : 31/02/2014 (JJ/MM/AAAA attendu)",
                "date de naissance dans le futur : 14/03/2027",
                "école inconnue : ECO-XXX",
                "classe invalide : 6EME (PRESCOLAIRE, CI, CP, CE1, CE2, CM1, CM2)",
                "champ obligatoire manquant : nom_mere",
                "téléphone invalide : +221 suivi de 9 chiffres attendu",
                "langue de contact invalide : ANGLAIS (FRANCAIS, WOLOF, PULAAR ou SEREER)");
        verify(eleves, never()).save(any());
    }

    @Test
    void refuse_un_en_tete_sans_les_colonnes_obligatoires() {
        assertThatThrownBy(() -> service.importer(fichier("INE;nom;prenom;classe;ecole_id;date;sexe\n1;Diop;Awa;CM2;1;"
                + "2014-01-01;F"), ecole))
                .isInstanceOfSatisfying(RegleMetierException.class, e -> assertThat(e.champs())
                        .containsExactly("date_naissance", "commune_naissance", "code_ecole", "nom_mere"));
    }

    @Test // L'ancien format « CSV » d'Excel (Windows-1252) casse les accents : le fichier est refusé
    void refuse_un_fichier_qui_n_est_pas_en_utf8() {
        String csv = ENTETE + "\nAïssatou;Bâ;F;14/03/2014;;Ngoye;ECO-BAM-01;CM2;Mariama Séne;;;;";

        assertThatThrownBy(() -> service.importer(fichier(csv, Charset.forName("windows-1252")), ecole))
                .isInstanceOfSatisfying(RegleMetierException.class, e -> assertThat(e.code()).isEqualTo("encodage"));
    }

    @Test
    void refuse_un_fichier_de_plus_de_1000_lignes() {
        String ligne = "\nAwa;Diouf;F;14/03/2014;;Ngoye;ECO-BAM-01;CM2;Mariama Sene;;;;";

        assertThatThrownBy(() -> service.importer(fichier(ENTETE + ligne.repeat(1001)), ecole))
                .isInstanceOfSatisfying(RegleMetierException.class,
                        e -> assertThat(e.code()).isEqualTo("fichier-trop-long"));
        verify(eleves, never()).save(any());
    }
}
