package sn.samatur.samatur.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.TestIntegration;

import static org.assertj.core.api.Assertions.assertThat;

/** Migrations V1 à V4 appliquées sur une base neuve : données de démonstration et séquences. */
class DonneesDemoTest extends TestIntegration {

    @Autowired
    JdbcTemplate jdbc;

    private long compter(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    @Test // Correction 2.10
    void quarante_dossiers_sur_les_six_etapes_dont_au_moins_quatre_en_retard() {
        assertThat(compter("SELECT count(*) FROM dossier")).isEqualTo(40);
        assertThat(compter("SELECT count(DISTINCT etape) FROM dossier")).isEqualTo(6);
        // RG-05 : à JUGEMENT_RENDU et jugé depuis plus que le délai cible de 15 jours
        assertThat(compter("""
                SELECT count(*) FROM dossier
                WHERE etape = 'JUGEMENT_RENDU' AND date_jugement < CURRENT_DATE - 15
                """)).isGreaterThanOrEqualTo(4);
    }

    @Test // RG-04 : les données de démo respectent elles-mêmes la chronologie
    void les_dates_des_etapes_se_suivent_et_aucune_n_est_dans_le_futur() {
        assertThat(compter("""
                SELECT count(*) FROM dossier
                WHERE date_certificat < date_detection
                   OR date_jugement < date_certificat
                   OR date_transcription < date_jugement
                   OR date_remise < date_transcription
                   OR GREATEST(date_detection, date_certificat, date_jugement,
                               date_transcription, date_remise) > CURRENT_DATE
                """)).isZero();
    }

    @Test // Correction 2.9, RG-08
    void les_references_suivent_le_format_ST_AAAA_NNNNNN() {
        assertThat(compter("SELECT count(*) FROM dossier WHERE reference !~ '^ST-[0-9]{4}-[0-9]{6}$'"))
                .isZero();
    }

    @Test // Cahier des charges, section 3 : exactement deux témoins quand la preuve est TEMOINS
    void chaque_dossier_avec_temoins_en_a_exactement_deux() {
        assertThat(compter("""
                SELECT count(*) FROM dossier d
                WHERE d.preuve_naissance = 'TEMOINS'
                  AND (SELECT count(*) FROM temoin t WHERE t.dossier_id = d.id) <> 2
                """)).isZero();
    }

    @Test // Correction 2.3 : sans setval, chaque insertion ci-dessous échouerait sur une clé en double
    @Transactional
    void les_sequences_sont_recalees_apres_les_insertions_a_id_fixe() {
        Long commune = jdbc.queryForObject(
                "INSERT INTO commune (nom) VALUES ('Lambaye') RETURNING id", Long.class);
        Long eleve = jdbc.queryForObject("""
                INSERT INTO eleve (prenom, nom, sexe, date_naissance, ecole_id, commune_naissance_id, nom_mere)
                VALUES ('Test', 'Sequence', 'F', DATE '2014-01-01', 1, ?, 'Mere Test') RETURNING id
                """, Long.class, commune);
        Long dossier = jdbc.queryForObject("""
                INSERT INTO dossier (reference, eleve_id)
                VALUES ('ST-2026-' || lpad(nextval('dossier_reference_seq')::text, 6, '0'), ?) RETURNING id
                """, Long.class, eleve);

        // Au-delà des id fixes de V2 (4 communes) et de V3 (40 élèves, 40 dossiers).
        // Pas d'égalité stricte : une séquence n'est pas annulée par le rollback des autres tests.
        assertThat(commune).isGreaterThan(4L);
        assertThat(eleve).isGreaterThan(40L);
        assertThat(dossier).isGreaterThan(40L);
    }
}
