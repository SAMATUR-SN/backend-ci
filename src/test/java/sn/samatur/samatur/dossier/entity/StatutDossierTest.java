package sn.samatur.samatur.dossier.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import sn.samatur.samatur.TestIntegration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Correction 2.1 : l'enum Java et la contrainte SQL de dossier.etape portent
 * exactement les mêmes six étapes, dans le même ordre. Avant correction, la base
 * refusait CERTIFICAT_OBTENU et aucun dossier ne pouvait atteindre ACTE_REMIS.
 */
class StatutDossierTest extends TestIntegration {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void l_enum_correspond_aux_six_valeurs_de_la_contrainte_sql() {
        String contrainte = jdbc.queryForObject("""
                SELECT pg_get_constraintdef(c.oid)
                FROM pg_constraint c
                WHERE c.conrelid = 'dossier'::regclass AND c.contype = 'c'
                  AND pg_get_constraintdef(c.oid) LIKE '%etape%'
                """, String.class);

        List<String> valeursSql = new ArrayList<>();
        Matcher m = Pattern.compile("'([A-Z_]+)'").matcher(contrainte);
        while (m.find()) {
            valeursSql.add(m.group(1));
        }

        List<String> valeursJava = Arrays.stream(StatutDossier.values()).map(Enum::name).toList();
        assertThat(valeursJava).hasSize(6).containsExactlyElementsOf(valeursSql);
    }
}
