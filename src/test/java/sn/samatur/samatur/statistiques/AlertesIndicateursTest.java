package sn.samatur.samatur.statistiques;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.TestIntegration;
import sn.samatur.samatur.security.UserDetailsServiceImpl;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bloc 6.B et 6.C, sur les données de V3 (dates relatives au jour du test). Valeurs attendues calculées
 * à la main d'après V3 :
 * - en retard : dossiers 21 à 25 (jugés il y a 60, 45, 32, 25, 20 jours) ; le 26, jugé il y a 15 jours, ne l'est pas ;
 * - I1 : 11 dossiers transcrits (30 à 40), dont 6 dans les 15 jours (30, 32, 34, 36, 38, 40) ;
 * - I2 : remise − détection des dossiers 36 à 40 = 70, 90, 65, 80, 75 jours, médiane 75 ;
 * - I3 : 35 élèves avant ACTE_REMIS, dont 9 hors CM2, soit 26.
 */
@Transactional
class AlertesIndicateursTest extends TestIntegration {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UserDetailsServiceImpl comptes;

    private UserDetails compte(String identifiant) {
        return comptes.loadUserByUsername(identifiant);
    }

    @Test // RG-05 : seuls les jugements non transcrits au-delà du délai cible, du plus ancien au plus récent
    void les_alertes_listent_les_jugements_non_transcrits_au_dela_du_delai_cible() throws Exception {
        mvc.perform(get("/api/alertes").with(user(compte("superviseur@samatur.sn"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(21, 22, 23, 24, 25)))
                .andExpect(jsonPath("$[0].joursDepuisJugement").value(60))
                .andExpect(jsonPath("$[0].enRetard").value(true));

        // Filtre de commune : seul le dossier 21 concerne un élève né à Ndangalma (commune 4)
        mvc.perform(get("/api/alertes").param("communeId", "4").with(user(compte("superviseur@samatur.sn"))))
                .andExpect(jsonPath("$[*].id", contains(21)));
    }

    @Test // RG-05 : jugé il y a 15 jours, pas en retard ; 16 jours, en retard
    void un_jugement_de_16_jours_est_en_retard_pas_un_de_15() throws Exception {
        jdbc.update("UPDATE dossier SET date_jugement = CURRENT_DATE - 16 WHERE id = 27");

        mvc.perform(get("/api/alertes").with(user(compte("superviseur@samatur.sn"))))
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].id", hasItem(27)))
                .andExpect(jsonPath("$[*].id", not(hasItem(26))));
    }

    @Test // RG-11 : deux vérifications successives ne créent pas de doublon
    void deux_verifications_ne_creent_qu_une_alerte_par_dossier() throws Exception {
        UserDetails etatCivil = compte("etatcivil1@samatur.sn");
        mvc.perform(post("/api/alertes/verifier").with(user(etatCivil)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alertesCreees").value(5));
        mvc.perform(post("/api/alertes/verifier").with(user(etatCivil)))
                .andExpect(jsonPath("$.alertesCreees").value(0));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM alerte", Long.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT message FROM alerte WHERE dossier_id = 21", String.class))
                .contains("délai cible de 15 jours");
    }

    @Test // F6 : I1, I2, I3, comptes par étape et retards, vérifiés à la main sur V3
    void les_indicateurs_globaux_correspondent_au_calcul_a_la_main() throws Exception {
        mvc.perform(get("/api/indicateurs").with(user(compte("superviseur@samatur.sn"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.i1PartTranscritsDansDelaiCible", closeTo(6.0 / 11, 1e-9)))
                .andExpect(jsonPath("$.i2DelaiMedianJours").value(75.0))
                .andExpect(jsonPath("$.i3ElevesClasseExamenSansActe").value(26))
                .andExpect(jsonPath("$.dossiersEnRetard").value(5))
                .andExpect(jsonPath("$.totalDossiers").value(40))
                .andExpect(jsonPath("$.delaiCibleJours").value(15))
                .andExpect(jsonPath("$.dossiersParEtape.DETECTE").value(7))
                .andExpect(jsonPath("$.dossiersParEtape.CERTIFICAT_OBTENU").value(7))
                .andExpect(jsonPath("$.dossiersParEtape.DOSSIER_COMPLET").value(6))
                .andExpect(jsonPath("$.dossiersParEtape.JUGEMENT_RENDU").value(9))
                .andExpect(jsonPath("$.dossiersParEtape.TRANSCRIT").value(6))
                .andExpect(jsonPath("$.dossiersParEtape.ACTE_REMIS").value(5));
    }

    @Test // F6 par commune : Ngoye (commune 2), dossiers 3, 9, 14, 17, 19, 23, 29, 34, 39
    void les_indicateurs_se_filtrent_par_commune_de_naissance() throws Exception {
        mvc.perform(get("/api/indicateurs").param("communeId", "2").with(user(compte("superviseur@samatur.sn"))))
                .andExpect(jsonPath("$.i1PartTranscritsDansDelaiCible").value(0.5))   // 34 dans le délai, 39 non
                .andExpect(jsonPath("$.i2DelaiMedianJours").value(80.0))              // seul le 39 : 170 − 90
                .andExpect(jsonPath("$.i3ElevesClasseExamenSansActe").value(7))       // le 19 est en CM1
                .andExpect(jsonPath("$.dossiersEnRetard").value(1))                  // le 23
                .andExpect(jsonPath("$.totalDossiers").value(9));
    }

    @Test // RG-12 : la date de référence change I3 ; à 200 jours, aucun acte n'était encore remis
    void la_date_de_reference_deplace_le_compte_des_eleves_bloques() throws Exception {
        mvc.perform(get("/api/indicateurs").param("dateReference", LocalDate.now().minusDays(200).toString())
                        .with(user(compte("superviseur@samatur.sn"))))
                .andExpect(jsonPath("$.i3ElevesClasseExamenSansActe").value(30));   // + 36, 37, 39, 40 (CM2)
    }

    @Test // Médiane I2 sur 3 dossiers aux dates connues ; I1 et I2 valent null sans dossier concerné
    void mediane_sur_trois_dossiers_connus_et_null_sans_donnees() throws Exception {
        Long commune = jdbc.queryForObject("INSERT INTO commune (nom) VALUES ('Commune de test') RETURNING id", Long.class);
        int[][] delais = {{10, 5}, {20, 30}, {60, 10}};   // {remise − détection, transcription − jugement}
        for (int i = 0; i < delais.length; i++) {
            Long eleve = jdbc.queryForObject("""
                    INSERT INTO eleve (prenom, nom, sexe, date_naissance, ecole_id, commune_naissance_id, nom_mere, classe)
                    VALUES ('Test', 'Mediane', 'F', DATE '2014-01-01', 1, ?, 'Mere', 'CM2') RETURNING id
                    """, Long.class, commune);
            jdbc.update("""
                    INSERT INTO dossier (reference, eleve_id, etape, date_detection, date_jugement, date_transcription, date_remise)
                    VALUES (?, ?, 'ACTE_REMIS', DATE '2026-01-01', DATE '2026-01-02', DATE '2026-01-02' + ?, DATE '2026-01-01' + ?)
                    """, "ST-2026-99999" + i, eleve, delais[i][1], delais[i][0]);
        }

        mvc.perform(get("/api/indicateurs").param("communeId", commune.toString())
                        .with(user(compte("superviseur@samatur.sn"))))
                .andExpect(jsonPath("$.i2DelaiMedianJours").value(20.0))
                .andExpect(jsonPath("$.i1PartTranscritsDansDelaiCible", closeTo(2.0 / 3, 1e-9)));

        Long vide = jdbc.queryForObject("INSERT INTO commune (nom) VALUES ('Commune vide') RETURNING id", Long.class);
        mvc.perform(get("/api/indicateurs").param("communeId", vide.toString())
                        .with(user(compte("superviseur@samatur.sn"))))
                .andExpect(jsonPath("$.i1PartTranscritsDansDelaiCible", nullValue()))
                .andExpect(jsonPath("$.i2DelaiMedianJours", nullValue()))
                .andExpect(jsonPath("$.i3ElevesClasseExamenSansActe").value(0))
                .andExpect(jsonPath("$.totalDossiers").value(0));
    }
}
