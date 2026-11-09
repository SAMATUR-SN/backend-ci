package sn.samatur.samatur.security;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Émission et vérification des jetons JWT, signés en HS256 avec SAMATUR_JWT_SECRET.
 * La clé est la même pour toutes les instances : pendant la mise à jour sans coupure,
 * un jeton émis par l'ancienne instance reste valable sur la nouvelle.
 */
@Component
public class JwtUtils {

    /** Seul algorithme accepté, à la signature comme à la vérification. */
    static final MacAlgorithm ALGORITHME = Jwts.SIG.HS256;

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private final SecretKey cle;
    private final long dureeMs;
    private final JwtParser lecteur;

    public JwtUtils(@Value("${samatur.jwt.secret}") String secret,
                    @Value("${samatur.jwt.expiration:28800000}") long dureeMs) {
        // Refuse au démarrage une clé de moins de 32 octets (256 bits), trop courte pour HS256
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.dureeMs = dureeMs;
        this.lecteur = Jwts.parser()
                .keyLocator(new LocatorAdapter<Key>() {
                    @Override
                    protected Key locate(JwsHeader entete) {
                        // Sans ce contrôle, JJWT choisirait l'algorithme annoncé par l'en-tête du jeton
                        // (HS384, HS512…) dès que la clé est assez longue.
                        if (!ALGORITHME.getId().equals(entete.getAlgorithm())) {
                            throw new UnsupportedJwtException("Algorithme de signature refusé : " + entete.getAlgorithm());
                        }
                        return cle;
                    }
                })
                .build();
    }

    public String generateJwtToken(UserDetails userDetails) {
        Date maintenant = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(maintenant)
                .expiration(new Date(maintenant.getTime() + dureeMs))
                .signWith(cle, ALGORITHME)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return lecteur.parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            lecteur.parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Jamais le jeton lui-même dans les logs
            logger.debug("Jeton JWT refusé : {}", e.getMessage());
        }
        return false;
    }

    public String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
