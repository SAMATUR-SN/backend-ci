package sn.samatur.samatur;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.security.UserDetailsServiceImpl;

import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bloc 5 : l'API suit le contrat contre lequel Khalil code le frontend (cahier des charges, section 6). */
class ContratApiTest extends TestIntegration {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserDetailsServiceImpl comptes;

    private UserDetails superviseur() {
        return comptes.loadUserByUsername("superviseur@samatur.sn");
    }

    @Test // 5.1, 5.3
    void la_liste_des_dossiers_est_une_page_de_resumes_sans_champ_interne() throws Exception {
        mvc.perform(get("/api/dossiers").param("etape", "DETECTE").param("taille", "5").with(user(superviseur())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", hasSize(5)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.taille").value(5))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.contenu[*].etape", everyItem(is("DETECTE"))))
                .andExpect(jsonPath("$.contenu[0].reference", matchesPattern("ST-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.contenu[0].eleve.prenom", notNullValue()))
                .andExpect(jsonPath("$.contenu[0].eleve.ecole", notNullValue()))
                .andExpect(jsonPath("$.contenu[0]", not(hasKey("version"))));
    }

    @Test // 5.3 : filtres sur les données de V3
    void les_filtres_s_appliquent() throws Exception {
        Map<String, Integer> attendus = Map.of(
                "enRetard=true", 5,     // dossiers 21 à 25, jugés depuis plus de 15 jours
                "communeId=2", 9,       // élèves nés à Ngoye
                "ecoleId=1", 9,
                "classe=CM1", 5,
                "q=diop", 2);           // nom ou prénom, sans casse : Mamadou et Elhadji Diop
        for (var cas : attendus.entrySet()) {
            String[] param = cas.getKey().split("=");
            mvc.perform(get("/api/dossiers").param(param[0], param[1]).with(user(superviseur())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(cas.getValue()));
        }
        mvc.perform(get("/api/dossiers").param("enRetard", "true").with(user(superviseur())))
                .andExpect(jsonPath("$.contenu[*].enRetard", everyItem(is(true))));
    }

    @Test
    void la_taille_est_plafonnee_et_les_parametres_invalides_donnent_400() throws Exception {
        mvc.perform(get("/api/dossiers").param("taille", "500").with(user(superviseur())))
                .andExpect(jsonPath("$.taille").value(100));
        mvc.perform(get("/api/dossiers").param("tri", "motDePasse,asc").with(user(superviseur())))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/dossiers").param("page", "-1").with(user(superviseur())))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/dossiers").param("etape", "INCONNUE").with(user(superviseur())))
                .andExpect(status().isBadRequest());
    }

    @Test // 5.5
    void les_quatre_referentiels_sont_exposes() throws Exception {
        mvc.perform(get("/api/referentiels/communes").with(user(superviseur())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(get("/api/referentiels/ecoles").with(user(superviseur())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].code", notNullValue()))
                .andExpect(jsonPath("$[0].communeId", notNullValue()));
        mvc.perform(get("/api/referentiels/centres").with(user(superviseur())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(get("/api/referentiels/tribunaux").with(user(superviseur())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/referentiels/communes")).andExpect(status().isUnauthorized());
    }

    @Test // 5.6
    void moi_renvoie_le_role_et_la_structure_de_chacun_des_quatre_comptes() throws Exception {
        Map<String, String[]> attendus = Map.of(
                "ecole1@samatur.sn", new String[]{"ECOLE", "École Publique Bambey Station 1"},
                "etatcivil1@samatur.sn", new String[]{"ETAT_CIVIL", "Centre Principal d'État Civil de Bambey"},
                "tribunal1@samatur.sn", new String[]{"TRIBUNAL", "Tribunal d'Instance de Bambey"},
                "superviseur@samatur.sn", new String[]{"SUPERVISEUR", null});
        for (var cas : attendus.entrySet()) {
            mvc.perform(get("/api/auth/moi").with(user(comptes.loadUserByUsername(cas.getKey()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.identifiant").value(cas.getKey()))
                    .andExpect(jsonPath("$.role").value(cas.getValue()[0]))
                    .andExpect(jsonPath("$.structure").value(cas.getValue()[1]));
        }
        mvc.perform(get("/api/auth/moi")).andExpect(status().isUnauthorized());
    }

    @Test // 5.6
    @Transactional
    void la_connexion_renvoie_jeton_expiration_et_utilisateur(@Autowired JdbcTemplate jdbc,
                                                             @Autowired PasswordEncoder encodeur) throws Exception {
        jdbc.update("""
                INSERT INTO utilisateur (identifiant, mot_de_passe_hash, nom_affiche, role, actif, ecole_id)
                VALUES ('contrat@test.sn', ?, 'Direction de test', 'ECOLE', TRUE, 1)
                """, encodeur.encode("Mot-de-passe-de-test"));

        mvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"contrat@test.sn\",\"motDePasse\":\"Mot-de-passe-de-test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton", notNullValue()))
                .andExpect(jsonPath("$.expireA", matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
                .andExpect(jsonPath("$.utilisateur.identifiant").value("contrat@test.sn"))
                .andExpect(jsonPath("$.utilisateur.nomAffiche").value("Direction de test"))
                .andExpect(jsonPath("$.utilisateur.role").value("ECOLE"))
                .andExpect(jsonPath("$.utilisateur.structure").value("École Publique Bambey Station 1"))
                .andExpect(jsonPath("$", not(hasKey("token"))));
    }

    @Test // 5.4, 5.7, décision du 09/11
    void les_routes_hors_contrat_ont_disparu() throws Exception {
        UserDetails ecole = comptes.loadUserByUsername("ecole1@samatur.sn");
        mvc.perform(delete("/api/eleves/1").with(user(ecole))).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/eleves").with(user(ecole)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/statistiques/dashboard").with(user(superviseur()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/indicateurs").with(user(superviseur()))).andExpect(status().isOk());
    }

    @Test // 3.9 : sans open-in-view, les réponses sont des DTO construits dans les transactions
    void les_alertes_et_les_eleves_se_serialisent_sans_session_ouverte() throws Exception {
        mvc.perform(get("/api/alertes").with(user(superviseur()))).andExpect(status().isOk());
        mvc.perform(get("/api/eleves/1").with(user(superviseur())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecole").value("École Publique Bambey Station 1"))
                .andExpect(jsonPath("$", not(hasKey("professionPere"))));
        mvc.perform(get("/api/eleves/9999").with(user(superviseur()))).andExpect(status().isNotFound());
    }

    @Test // RG-13 : aucun dossier, élève ni historique n'est supprimé physiquement
    void aucune_route_de_l_api_ne_supprime() throws Exception {
        String specification = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(specification).doesNotContain("\"delete\"");
    }

    @Test // Swagger reste public et ne décrit plus les routes retirées
    void la_documentation_openapi_est_publique_et_a_jour() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths", hasKey("/api/auth/moi")))
                .andExpect(jsonPath("$.paths", hasKey("/api/indicateurs")))
                .andExpect(jsonPath("$.paths", hasKey("/api/referentiels/tribunaux")))
                .andExpect(jsonPath("$.paths", not(hasKey("/api/statistiques/dashboard"))))
                .andExpect(jsonPath("$.paths['/api/eleves']", not(hasKey("post"))));
    }
}
