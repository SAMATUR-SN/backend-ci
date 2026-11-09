package sn.samatur.samatur.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.TestIntegration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Connexion et santé à travers toute la chaîne HTTP et de sécurité (bloc 3). */
@Transactional
class AuthentificationTest extends TestIntegration {

    private static final String ECHEC = "Identifiant ou mot de passe incorrect.";

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder encodeur;

    private void creerCompte(String identifiant, String motDePasse, boolean actif) {
        jdbc.update("""
                INSERT INTO utilisateur (identifiant, mot_de_passe_hash, nom_affiche, role, actif, ecole_id)
                VALUES (?, ?, 'Compte de test', 'ECOLE', ?, 1)
                """, identifiant, encodeur.encode(motDePasse), actif);
    }

    private ResultActions connexion(String identifiant, String motDePasse) throws Exception {
        return mvc.perform(post("/api/auth/connexion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifiant\":\"" + identifiant + "\",\"motDePasse\":\"" + motDePasse + "\"}"));
    }

    @Test
    void un_compte_actif_se_connecte() throws Exception {
        creerCompte("actif@test.sn", "Mot-de-passe-de-test-1", true);
        connexion("actif@test.sn", "Mot-de-passe-de-test-1").andExpect(status().isOk());
    }

    @Test // 3.2, RG-15
    void un_compte_desactive_ne_se_connecte_pas_meme_avec_le_bon_mot_de_passe() throws Exception {
        creerCompte("inactif@test.sn", "Mot-de-passe-de-test-2", false);
        connexion("inactif@test.sn", "Mot-de-passe-de-test-2")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(ECHEC));
    }

    @Test // 3.5, RG-15 : même réponse JSON que le compte existe ou non
    void mauvais_mot_de_passe_et_compte_inconnu_donnent_le_meme_401() throws Exception {
        creerCompte("actif2@test.sn", "Mot-de-passe-de-test-3", true);
        connexion("actif2@test.sn", "mauvais")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(ECHEC));
        connexion("inconnu@test.sn", "mauvais")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(ECHEC));
    }

    @Test // 7.5, RG-14
    void sans_jeton_l_api_repond_401() throws Exception {
        mvc.perform(get("/api/dossiers")).andExpect(status().isUnauthorized());
    }

    @Test // 3.1 : /health public, sans détail sur la base
    void health_est_public_et_repond_UP() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", org.springframework.test.json.JsonCompareMode.STRICT));
    }
}
