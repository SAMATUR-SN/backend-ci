package sn.samatur.samatur.alerte.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.samatur.samatur.alerte.repository.AlerteRepository;
import sn.samatur.samatur.alerte.service.AlerteService;
import sn.samatur.samatur.security.JwtUtils;
import sn.samatur.samatur.security.SecurityConfig;
import sn.samatur.samatur.security.UserDetailsServiceImpl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sécurité des routes d'alertes : 401 sans jeton (7.5), 403 pour un rôle non autorisé (3.7). */
@WebMvcTest(AlerteController.class)
@Import(SecurityConfig.class)
class AlerteSecuriteTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AlerteService alerteService;
    @MockitoBean
    AlerteRepository alerteRepository;
    @MockitoBean
    UserDetailsServiceImpl userDetailsService;
    @MockitoBean
    JwtUtils jwtUtils;

    @Test
    void sans_jeton_401() throws Exception {
        mvc.perform(get("/api/alertes")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ECOLE")
    void une_ecole_ne_resout_pas_une_alerte_403() throws Exception {
        mvc.perform(patch("/api/alertes/1/resoudre"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "SUPERVISEUR")
    void le_superviseur_en_lecture_seule_ne_declenche_pas_la_verification_403() throws Exception {
        mvc.perform(post("/api/alertes/verifier")).andExpect(status().isForbidden());
    }
}
