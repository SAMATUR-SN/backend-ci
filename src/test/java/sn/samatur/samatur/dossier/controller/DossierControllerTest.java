package sn.samatur.samatur.dossier.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.samatur.samatur.dossier.dto.AvancerDossierRequest;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.service.DossierService;
import sn.samatur.samatur.security.JwtUtils;
import sn.samatur.samatur.security.SecurityConfig;
import sn.samatur.samatur.security.UserDetailsImpl;
import sn.samatur.samatur.security.UserDetailsServiceImpl;
import sn.samatur.samatur.security.entity.Utilisateur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La route qui porte le métier : sans jeton 401, mauvais rôle 403, bon rôle succès (cahier des charges, section 7). */
@WebMvcTest(DossierController.class)
@Import(SecurityConfig.class)
class DossierControllerTest {

    private static final String JUGEMENT = """
            {"etapeCible":"JUGEMENT_RENDU","numeroJugement":"1287/2026","dateJugement":"2026-11-03",
             "tribunalId":1,"typeAudience":"FORAINE","role":"ECOLE"}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DossierService dossierService;
    @MockitoBean
    UserDetailsServiceImpl userDetailsService;
    @MockitoBean
    JwtUtils jwtUtils;

    private static UserDetailsImpl compte(long id, String role) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setIdentifiant(role.toLowerCase() + "@test.sn");
        u.setMotDePasseHash("x");
        u.setRole(role);
        u.setActif(true);
        return UserDetailsImpl.build(u);
    }

    @Test
    void sans_jeton_401() throws Exception {
        mvc.perform(post("/api/dossiers/1/avancer").contentType(MediaType.APPLICATION_JSON).content(JUGEMENT))
                .andExpect(status().isUnauthorized());
        verify(dossierService, never()).avancer(any(), any(), any());
    }

    @Test // Le superviseur consulte tout et ne modifie rien
    void le_superviseur_ne_fait_avancer_aucun_dossier_403() throws Exception {
        mvc.perform(post("/api/dossiers/1/avancer").with(user(compte(4, "SUPERVISEUR")))
                        .contentType(MediaType.APPLICATION_JSON).content(JUGEMENT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(dossierService, never()).avancer(any(), any(), any());
    }

    @Test // RG-16 : le rôle vient du compte connecté ; un champ « role » envoyé par le client est ignoré
    void le_bon_role_avance_et_le_role_envoye_par_le_client_est_ignore() throws Exception {
        mvc.perform(post("/api/dossiers/1/avancer").with(user(compte(3, "TRIBUNAL")))
                        .contentType(MediaType.APPLICATION_JSON).content(JUGEMENT))
                .andExpect(status().isOk());

        ArgumentCaptor<UserDetailsImpl> qui = ArgumentCaptor.forClass(UserDetailsImpl.class);
        ArgumentCaptor<AvancerDossierRequest> requete = ArgumentCaptor.forClass(AvancerDossierRequest.class);
        verify(dossierService).avancer(eq(1L), requete.capture(), qui.capture());
        assertThat(qui.getValue().getRole()).isEqualTo("TRIBUNAL");
        assertThat(requete.getValue().etapeCible()).isEqualTo(StatutDossier.JUGEMENT_RENDU);
    }

    @Test
    void un_json_illisible_donne_400() throws Exception {
        mvc.perform(post("/api/dossiers/1/avancer").with(user(compte(3, "TRIBUNAL")))
                        .contentType(MediaType.APPLICATION_JSON).content("{pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête illisible"));
    }
}
