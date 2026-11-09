package sn.samatur.samatur.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Jetons signés et vérifiés en HS256 uniquement, quelle que soit la longueur de la clé. */
class JwtUtilsTest {

    // Plus de 64 caractères : assez long pour que JJWT accepte aussi HS512 si on ne l'en empêchait pas
    private static final String SECRET = "cle-de-test-longue-de-plus-de-soixante-quatre-caracteres-pour-hs512-uniquement";

    private final JwtUtils jwt = new JwtUtils(SECRET, 3_600_000);

    @Test
    void signe_les_jetons_en_HS256() {
        String jeton = jwt.generateJwtToken(User.withUsername("ecole1@samatur.sn").password("x").roles("ECOLE").build());

        String entete = new String(Base64.getUrlDecoder().decode(jeton.split("\\.")[0]), StandardCharsets.UTF_8);
        assertThat(entete).contains("\"alg\":\"HS256\"");
        assertThat(jwt.validateJwtToken(jeton)).isTrue();
        assertThat(jwt.getUserNameFromJwtToken(jeton)).isEqualTo("ecole1@samatur.sn");
    }

    @Test
    void refuse_un_jeton_signe_avec_la_bonne_cle_mais_un_autre_algorithme() {
        String jetonHs512 = Jwts.builder()
                .subject("ecole1@samatur.sn")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512)
                .compact();

        assertThat(jwt.validateJwtToken(jetonHs512)).isFalse();
    }

    @Test
    void refuse_un_jeton_non_signe() {
        String jetonSansSignature = Jwts.builder().subject("ecole1@samatur.sn").compact();

        assertThat(jwt.validateJwtToken(jetonSansSignature)).isFalse();
    }

    @Test
    void refuse_au_demarrage_une_cle_trop_courte_pour_HS256() {
        assertThatThrownBy(() -> new JwtUtils("trop-courte", 3_600_000)).isInstanceOf(WeakKeyException.class);
    }
}
