package sn.samatur.samatur.eleve;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.TestIntegration;
import sn.samatur.samatur.security.UserDetailsServiceImpl;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bloc 6.A, « Comment vérifier » : import à travers HTTP, sur la base réelle (V1 à V4 et données de démo). */
@Transactional
class ImportCsvTest extends TestIntegration {

    private static final String ENTETE = "prenom;nom;sexe;date_naissance;date_naissance_presumee;commune_naissance;"
            + "code_ecole;classe;nom_mere;nom_pere;telephone_parent;langue_contact;numero_ine";

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UserDetailsServiceImpl comptes;

    private UserDetails ecole() {
        return comptes.loadUserByUsername("ecole1@samatur.sn");
    }

    private long compter(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    @Test
    void dix_lignes_dont_deux_invalides_donnent_huit_eleves_et_huit_dossiers_detectes() throws Exception {
        StringBuilder csv = new StringBuilder(ENTETE);
        String[] prenoms = {"Awa", "Aminata", "Ousmane", "Khady", "Cheikh", "Mame", "Pape", "Seynabou"};
        for (int i = 0; i < prenoms.length; i++) {
            csv.append("\n").append(prenoms[i]).append(";Test-Import;F;0").append(i + 1)
                    .append("/03/2014;non;Ngoye;ECO-NGO-01;CM2;Mere Test;;;;");
        }
        csv.append("\nMoussa;Ndiaye;M;02/07/2014;oui;Bambeye;ECO-BAM-01;CM2;Fatou Sarr;;;;");
        // Doublon d'un élève de V3 : Mamadou Diop, né le 12/05/2014, école ECO-BAM-01
        csv.append("\nMamadou;Diop;M;12/05/2014;;Bambey (Commune);ECO-BAM-01;CM2;Aissatou Ba;;;;");
        long detectesAvant = compter("SELECT count(*) FROM dossier WHERE etape = 'DETECTE'");

        mvc.perform(multipart("/api/eleves/import")
                        .file(new MockMultipartFile("file", "eleves.csv", "text/csv",
                                csv.toString().getBytes(StandardCharsets.UTF_8)))
                        .with(user(ecole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.importes").value(8))
                .andExpect(jsonPath("$.rejetes").value(2))
                .andExpect(jsonPath("$.erreurs[0].ligne").value(10))
                .andExpect(jsonPath("$.erreurs[0].motif").value("commune inconnue : Bambeye"))
                .andExpect(jsonPath("$.erreurs[1].ligne").value(11))
                .andExpect(jsonPath("$.erreurs[1].motif").value(
                        "doublon : un élève de même prénom, nom, date de naissance et école existe déjà"));

        assertThat(compter("SELECT count(*) FROM dossier WHERE etape = 'DETECTE'")).isEqualTo(detectesAvant + 8);
        // Chaque dossier créé a sa référence ST-AAAA-NNNNNN et sa première ligne d'historique
        assertThat(compter("""
                SELECT count(*) FROM dossier d JOIN eleve e ON e.id = d.eleve_id
                WHERE e.nom = 'Test-Import' AND d.reference ~ '^ST-[0-9]{4}-[0-9]{6}$'
                  AND d.date_detection = CURRENT_DATE
                  AND EXISTS (SELECT 1 FROM historique_etape h WHERE h.dossier_id = d.id
                              AND h.etape_arrivee = 'DETECTE' AND h.role_auteur = 'ECOLE' AND h.auteur_id = 1)
                """)).isEqualTo(8);
    }

    @Test // Le fichier d'exemple de la démo : Awa est importée, la ligne fausse est expliquée
    void le_fichier_d_exemple_importe_awa_et_rejette_la_ligne_fausse() throws Exception {
        byte[] contenu = new ClassPathResource("import-exemple.csv").getContentAsByteArray();

        mvc.perform(multipart("/api/eleves/import")
                        .file(new MockMultipartFile("file", "import-exemple.csv", "text/csv", contenu))
                        .with(user(ecole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importes").value(1))
                .andExpect(jsonPath("$.erreurs[0].motif").value("commune inconnue : Bambeye"));

        assertThat(compter("SELECT count(*) FROM eleve WHERE prenom = 'Awa' AND nom = 'Diouf'")).isEqualTo(1);
    }

    @Test
    void seule_une_ecole_importe_et_le_fichier_est_obligatoire() throws Exception {
        MockMultipartFile csv = new MockMultipartFile("file", "eleves.csv", "text/csv",
                (ENTETE + "\n").getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/eleves/import").file(csv)
                        .with(user(comptes.loadUserByUsername("tribunal1@samatur.sn"))))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/eleves/import").with(user(ecole())))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/eleves/import")
                        .file(new MockMultipartFile("file", "vide.csv", "text/csv", new byte[0]))
                        .with(user(ecole())))
                .andExpect(status().isBadRequest());
    }
}
