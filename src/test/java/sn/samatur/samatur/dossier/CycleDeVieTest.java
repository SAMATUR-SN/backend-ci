package sn.samatur.samatur.dossier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.TestIntegration;
import sn.samatur.samatur.security.UserDetailsServiceImpl;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bloc 4, « Comment vérifier » : un dossier franchit les six étapes avec les bons comptes de démo,
 * et chaque refus suit le contrat d'API (422, 409, 403, 404, 400). Base réelle, requêtes HTTP réelles.
 */
@Transactional
class CycleDeVieTest extends TestIntegration {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserDetailsServiceImpl comptes;

    private final String aujourdhui = LocalDate.now().toString();

    private UserDetails compte(String identifiant) {
        return comptes.loadUserByUsername(identifiant);
    }

    private ResultActions avancer(long dossier, String identifiant, String json) throws Exception {
        return mvc.perform(post("/api/dossiers/" + dossier + "/avancer").with(user(compte(identifiant)))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void un_dossier_franchit_les_six_etapes_avec_les_bons_comptes() throws Exception {
        // Dossier 1 de V3 : DETECTE depuis 3 jours
        avancer(1, "etatcivil1@samatur.sn", """
                {"etapeCible":"CERTIFICAT_OBTENU","numeroCertificat":"CERT-9001","dateCertificat":"%s","centreEtatCivilId":1}
                """.formatted(aujourdhui))
                .andExpect(status().isOk()).andExpect(jsonPath("$.etape").value("CERTIFICAT_OBTENU"));
        avancer(1, "ecole1@samatur.sn", """
                {"etapeCible":"DOSSIER_COMPLET","preuveNaissance":"TEMOINS","temoins":[
                  {"nomComplet":"Ibrahima Ndiaye","numeroPiece":"CNI-1"},{"nomComplet":"Fatou Diop","numeroPiece":"CNI-2"}]}
                """)
                .andExpect(status().isOk()).andExpect(jsonPath("$.temoins", hasSize(2)));
        avancer(1, "tribunal1@samatur.sn", """
                {"etapeCible":"JUGEMENT_RENDU","numeroJugement":"9001/2026","dateJugement":"%s","tribunalId":1,"typeAudience":"FORAINE"}
                """.formatted(aujourdhui))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enRetard").value(false));
        avancer(1, "etatcivil1@samatur.sn", """
                {"etapeCible":"TRANSCRIT","numeroActe":"9001","anneeActe":%d,"dateTranscription":"%s"}
                """.formatted(LocalDate.now().getYear(), aujourdhui))
                .andExpect(status().isOk());
        avancer(1, "ecole1@samatur.sn", """
                {"etapeCible":"ACTE_REMIS","dateRemise":"%s","commentaire":"Acte remis à la famille"}
                """.formatted(aujourdhui))
                .andExpect(status().isOk()).andExpect(jsonPath("$.etape").value("ACTE_REMIS"));

        // L'historique montre les six passages, avec auteur, rôle et étapes de départ et d'arrivée
        mvc.perform(get("/api/dossiers/1").with(user(compte("superviseur@samatur.sn"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historique", hasSize(6)))
                .andExpect(jsonPath("$.historique[*].etapeArrivee", contains("DETECTE", "CERTIFICAT_OBTENU",
                        "DOSSIER_COMPLET", "JUGEMENT_RENDU", "TRANSCRIT", "ACTE_REMIS")))
                .andExpect(jsonPath("$.historique[*].role", contains("ECOLE", "ETAT_CIVIL", "ECOLE", "TRIBUNAL",
                        "ETAT_CIVIL", "ECOLE")))
                .andExpect(jsonPath("$.historique[5].etapeDepart").value("TRANSCRIT"))
                .andExpect(jsonPath("$.historique[5].auteur").value("Directeur École Bambey Station"));
    }

    @Test
    void les_refus_suivent_le_contrat_d_api() throws Exception {
        // Dossier 15 de V3 : DOSSIER_COMPLET
        avancer(15, "ecole1@samatur.sn", """
                {"etapeCible":"JUGEMENT_RENDU","numeroJugement":"9002/2026","dateJugement":"%s","tribunalId":1,"typeAudience":"FORAINE"}
                """.formatted(aujourdhui))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        avancer(15, "tribunal1@samatur.sn", "{\"etapeCible\":\"TRANSCRIT\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("https://samatur.sn/erreurs/saut-etape"));
        avancer(15, "tribunal1@samatur.sn", "{\"etapeCible\":\"JUGEMENT_RENDU\",\"numeroJugement\":\"9002/2026\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Champ obligatoire manquant"))
                .andExpect(jsonPath("$.erreurs[*].champ", contains("dateJugement", "tribunalId", "typeAudience")));
        avancer(15, "ecole1@samatur.sn", "{\"etapeCible\":\"DOSSIER_COMPLET\",\"preuveNaissance\":\"CERTIFICAT_ACCOUCHEMENT\"}")
                .andExpect(status().isConflict());
        avancer(15, "tribunal1@samatur.sn", "{}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erreurs[0].champ").value("etapeCible"));
        avancer(15, "tribunal1@samatur.sn", "{\"etapeCible\":\"CERTIFICAT_ETABLI\"}")
                .andExpect(status().isBadRequest());
        avancer(9999, "tribunal1@samatur.sn", "{\"etapeCible\":\"JUGEMENT_RENDU\"}")
                .andExpect(status().isNotFound());
        avancer(15, "superviseur@samatur.sn", "{\"etapeCible\":\"JUGEMENT_RENDU\"}")
                .andExpect(status().isForbidden());
    }
}
