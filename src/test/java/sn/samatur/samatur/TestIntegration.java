package sn.samatur.samatur;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Socle des tests d'intégration : application complète sur une base PostgreSQL 18
 * neuve, où Flyway applique toutes les migrations. Les classes qui en héritent
 * partagent le même contexte Spring, donc un seul conteneur. MockMvc rejoue les requêtes HTTP
 * à travers toute la chaîne de sécurité.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class TestIntegration {
}
