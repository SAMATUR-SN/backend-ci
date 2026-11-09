package sn.samatur.samatur.eleve;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.TestIntegration;
import sn.samatur.samatur.security.UserDetailsServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fonction facultative : gestion des élèves par le directeur d'école. ecole1 est rattaché à l'école 1
 * (École Publique Bambey Station 1), où V3 place les élèves 1, 2, 8, 13, 17, 22, 27, 32 et 37.
 */
@Transactional
class EleveEcoleTest extends TestIntegration {

    private static final String AWA = """
            {"prenom":"Awa","nom":"Saisie","sexe":"F","dateNaissance":"2014-03-14","communeNaissanceId":1,
             "classe":"CM2","nomMere":"Mariama Sene","telephoneParent":"+221770000099","langueContact":"WOLOF"}
            """;

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UserDetailsServiceImpl comptes;
    @PersistenceContext
    EntityManager entityManager;

    private UserDetails ecole() {
        return comptes.loadUserByUsername("ecole1@samatur.sn");
    }

    private long compter(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    @Test
    void le_directeur_ne_voit_que_les_eleves_de_son_ecole() throws Exception {
        mvc.perform(get("/api/eleves").with(user(ecole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(9))
                .andExpect(jsonPath("$.contenu[*].ecoleId", everyItem(is(1))));
        mvc.perform(get("/api/eleves/1").with(user(ecole()))).andExpect(status().isOk());
        // L'élève 3 est à l'école 3 : introuvable, pour ne pas révéler son existence
        mvc.perform(get("/api/eleves/3").with(user(ecole()))).andExpect(status().isNotFound());
        mvc.perform(put("/api/eleves/3").with(user(ecole())).contentType(MediaType.APPLICATION_JSON).content(AWA))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/eleves/3").with(user(ecole()))).andExpect(status().isNotFound());
    }

    @Test
    void les_autres_roles_recoivent_403() throws Exception {
        UserDetails etatCivil = comptes.loadUserByUsername("etatcivil1@samatur.sn");
        mvc.perform(get("/api/eleves").with(user(etatCivil))).andExpect(status().isForbidden());
        mvc.perform(post("/api/eleves").with(user(etatCivil)).contentType(MediaType.APPLICATION_JSON).content(AWA))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/eleves/1").with(user(comptes.loadUserByUsername("superviseur@samatur.sn"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void une_creation_produit_un_dossier_detecte_par_la_meme_methode_que_l_import() throws Exception {
        mvc.perform(post("/api/eleves").with(user(ecole())).contentType(MediaType.APPLICATION_JSON).content(AWA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Saisie"))
                .andExpect(jsonPath("$.ecoleId").value(1))
                .andExpect(jsonPath("$.langueContact").value("WOLOF"));

        assertThat(compter("""
                SELECT count(*) FROM dossier d JOIN eleve e ON e.id = d.eleve_id
                WHERE e.nom = 'Saisie' AND d.etape = 'DETECTE' AND d.reference ~ '^ST-[0-9]{4}-[0-9]{6}$'
                  AND EXISTS (SELECT 1 FROM historique_etape h WHERE h.dossier_id = d.id AND h.etape_arrivee = 'DETECTE')
                """)).isEqualTo(1);

        // RG-07 : la même saisie une seconde fois est un doublon
        mvc.perform(post("/api/eleves").with(user(ecole())).contentType(MediaType.APPLICATION_JSON).content(AWA))
                .andExpect(status().isConflict());
    }

    @Test
    void une_saisie_invalide_donne_422_avec_la_liste_des_champs() throws Exception {
        mvc.perform(post("/api/eleves").with(user(ecole())).contentType(MediaType.APPLICATION_JSON).content("""
                        {"prenom":"Awa","nom":"Saisie","sexe":"X","dateNaissance":"2014-03-14","communeNaissanceId":1,
                         "classe":"CM2","telephoneParent":"770000001"}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erreurs[*].champ", hasItem("nomMere")))
                .andExpect(jsonPath("$.erreurs[*].champ", hasItem("sexe")))
                .andExpect(jsonPath("$.erreurs[*].champ", hasItem("telephoneParent")));
        mvc.perform(post("/api/eleves").with(user(ecole())).contentType(MediaType.APPLICATION_JSON)
                        .content(AWA.replace("\"communeNaissanceId\":1", "\"communeNaissanceId\":999")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erreurs[0].champ").value("communeNaissanceId"));
    }

    @Test // Règle proposée : l'identité ne change plus une fois le dossier au-delà de DETECTE
    void l_identite_ne_change_plus_apres_detecte_mais_le_contact_si() throws Exception {
        // Élève 1 (Mamadou Diop) : dossier DETECTE, l'identité et la commune de naissance peuvent changer
        mvc.perform(put("/api/eleves/1").with(user(ecole())).contentType(MediaType.APPLICATION_JSON).content("""
                        {"prenom":"Mamadou","nom":"Diop Corrigé","sexe":"M","dateNaissance":"2014-05-12",
                         "communeNaissanceId":2,"classe":"CM2","nomMere":"Aissatou Ba"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Diop Corrigé"))
                .andExpect(jsonPath("$.communeNaissanceId").value(2));

        // Élève 8 (Mariama Ba) : dossier CERTIFICAT_OBTENU, le nom ne change plus…
        mvc.perform(put("/api/eleves/8").with(user(ecole())).contentType(MediaType.APPLICATION_JSON).content("""
                        {"prenom":"Mariama","nom":"Bah","sexe":"F","dateNaissance":"2014-04-03",
                         "communeNaissanceId":1,"classe":"CM2","nomMere":"Fatimata Sy"}
                        """))
                .andExpect(status().isConflict());
        // … ni la commune de naissance, qui désigne le centre d'état civil compétent (décision du 09/11)…
        mvc.perform(put("/api/eleves/8").with(user(ecole())).contentType(MediaType.APPLICATION_JSON).content("""
                        {"prenom":"Mariama","nom":"Ba","sexe":"F","dateNaissance":"2014-04-03",
                         "communeNaissanceId":2,"classe":"CM2","nomMere":"Fatimata Sy"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Le dossier a dépassé l'étape DETECTE : nom, prénom, "
                        + "date de naissance et commune de naissance ne peuvent plus changer."));
        // … mais le téléphone et la langue de contact, si
        mvc.perform(put("/api/eleves/8").with(user(ecole())).contentType(MediaType.APPLICATION_JSON).content("""
                        {"prenom":"Mariama","nom":"Ba","sexe":"F","dateNaissance":"2014-04-03",
                         "communeNaissanceId":1,"classe":"CM2","nomMere":"Fatimata Sy",
                         "telephoneParent":"+221770000088","langueContact":"PULAAR"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephoneParent").value("+221770000088"));
    }

    @Test // RG-13 : un archivage ne supprime aucune ligne
    void un_archivage_ne_supprime_aucune_ligne_et_sort_l_eleve_des_listes_alertes_et_indicateurs() throws Exception {
        String totaux = "SELECT (SELECT count(*) FROM eleve) || '/' || (SELECT count(*) FROM dossier) || '/' "
                + "|| (SELECT count(*) FROM historique_etape)";
        String avant = jdbc.queryForObject(totaux, String.class);
        UserDetails superviseur = comptes.loadUserByUsername("superviseur@samatur.sn");

        // Élève 2 (Fatou Sarr) : école 1, CM2, dossier encore à DETECTE
        mvc.perform(delete("/api/eleves/2").with(user(ecole()))).andExpect(status().isNoContent());
        // Le test tourne dans une seule transaction : on pousse les écritures en base avant de lire en SQL
        entityManager.flush();

        assertThat(jdbc.queryForObject(totaux, String.class)).isEqualTo(avant);
        assertThat(compter("SELECT count(*) FROM eleve WHERE id = 2 AND archive_le IS NOT NULL")).isEqualTo(1);
        mvc.perform(get("/api/eleves/2").with(user(ecole()))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/eleves/2").with(user(ecole()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/dossiers/2").with(user(superviseur))).andExpect(status().isNotFound());
        mvc.perform(get("/api/dossiers").with(user(superviseur))).andExpect(jsonPath("$.totalElements").value(39));
        mvc.perform(get("/api/indicateurs").with(user(superviseur)))
                .andExpect(jsonPath("$.totalDossiers").value(39))
                .andExpect(jsonPath("$.dossiersParEtape.DETECTE").value(6))
                .andExpect(jsonPath("$.i3ElevesClasseExamenSansActe").value(25));
    }

    @Test // Décision du 09/11 : on n'archive plus un élève dont le dossier a dépassé DETECTE
    void un_eleve_dont_le_dossier_a_depasse_detecte_ne_s_archive_pas() throws Exception {
        // Élève 22 : école 1, dossier à JUGEMENT_RENDU, en retard
        mvc.perform(delete("/api/eleves/22").with(user(ecole())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Le dossier a dépassé l'étape DETECTE : il est suivi par "
                        + "l'état civil ou le tribunal, l'élève ne peut plus être archivé."));
        entityManager.flush();

        assertThat(compter("SELECT count(*) FROM eleve WHERE id = 22 AND archive_le IS NULL")).isEqualTo(1);
        mvc.perform(get("/api/alertes").with(user(comptes.loadUserByUsername("superviseur@samatur.sn"))))
                .andExpect(jsonPath("$[*].id", hasItem(22)));
    }
}
