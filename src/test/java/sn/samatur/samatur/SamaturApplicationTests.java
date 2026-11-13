package sn.samatur.samatur;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Le contexte démarre : Flyway applique V1 à V4 et Hibernate valide le schéma (ddl-auto=validate). */
class SamaturApplicationTests extends TestIntegration {

    @Test
    void contextLoads(ApplicationContext context) {
        assertNotNull(context);
    }
}
