package sn.samatur.samatur.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.samatur.samatur.TestIntegration;
import sn.samatur.samatur.security.entity.Utilisateur;
import sn.samatur.samatur.security.repository.UtilisateurRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Correction 2.4 : les quatre comptes de démo ont de vrais hachages BCrypt et peuvent se connecter. */
class ComptesDemoTest extends TestIntegration {

    private static final Map<String, String> ROLES = Map.of(
            "ecole1@samatur.sn", "ECOLE",
            "etatcivil1@samatur.sn", "ETAT_CIVIL",
            "tribunal1@samatur.sn", "TRIBUNAL",
            "superviseur@samatur.sn", "SUPERVISEUR");

    @Autowired
    UtilisateurRepository utilisateurs;
    @Autowired
    PasswordEncoder encodeur;
    @Autowired
    AuthenticationManager authentification;

    @Test
    void les_quatre_comptes_ont_un_hachage_bcrypt_distinct_et_aucun_mot_de_passe_trivial() {
        Set<String> hachages = new HashSet<>();
        ROLES.forEach((identifiant, role) -> {
            Utilisateur u = utilisateurs.findByIdentifiant(identifiant).orElseThrow();
            assertThat(u.getRole()).isEqualTo(role);
            assertThat(u.getActif()).isTrue();
            assertThat(u.getMotDePasseHash()).matches("^\\$2[aby]\\$1[0-9]\\$[./A-Za-z0-9]{53}$");
            assertThat(encodeur.matches("password123", u.getMotDePasseHash())).isFalse();
            hachages.add(u.getMotDePasseHash());
        });
        assertThat(hachages).hasSize(4);
    }

    /**
     * Les mots de passe ne sont jamais dans le dépôt : ils arrivent par la variable
     * SAMATUR_DEMO_MOTS_DE_PASSE (identifiant=mot de passe, séparés par des virgules),
     * fournie par le .env local ou par un credential Jenkins. Sans elle, le test est ignoré.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "SAMATUR_DEMO_MOTS_DE_PASSE", matches = ".+")
    void chaque_compte_de_demo_se_connecte_avec_son_mot_de_passe() {
        Set<String> verifies = new HashSet<>();
        for (String paire : System.getenv("SAMATUR_DEMO_MOTS_DE_PASSE").split(",")) {
            String[] cle = paire.trim().split("=", 2);
            Authentication a = authentification.authenticate(
                    new UsernamePasswordAuthenticationToken(cle[0], cle[1]));
            assertThat(a.isAuthenticated()).isTrue();
            // Spring Security 7 ajoute aussi une autorité FACTOR_PASSWORD (facteur utilisé) : on vérifie le rôle
            assertThat(a.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_" + ROLES.get(cle[0]));
            verifies.add(cle[0]);
        }
        assertThat(verifies).isEqualTo(ROLES.keySet());
    }
}
