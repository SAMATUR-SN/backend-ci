package sn.samatur.samatur.dossier.entity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatutDossierTest {

    @Test
    void statusesMatchTheDatabaseConstraint() {
        assertEquals(
                List.of("DETECTE", "CERTIFICAT_ETABLI", "DOSSIER_COMPLET",
                        "JUGEMENT_RENDU", "TRANSCRIT", "REMIS"),
                Arrays.stream(StatutDossier.values()).map(Enum::name).toList());
    }
}
