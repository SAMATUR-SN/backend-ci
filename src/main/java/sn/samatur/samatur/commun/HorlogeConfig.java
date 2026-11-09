package sn.samatur.samatur.commun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Une horloge unique pour toute l'application. Le code métier ne lit jamais
 * LocalDate.now() directement : les tests remplacent cette horloge par une date fixe.
 */
@Configuration
public class HorlogeConfig {

    @Bean
    public Clock horloge() {
        return Clock.system(ZoneId.of("Africa/Dakar"));
    }
}
