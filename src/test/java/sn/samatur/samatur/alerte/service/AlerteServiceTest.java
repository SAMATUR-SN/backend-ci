package sn.samatur.samatur.alerte.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sn.samatur.samatur.alerte.entity.Alerte;
import sn.samatur.samatur.alerte.repository.AlerteRepository;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.repository.DossierRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RG-11 et RG-06, sans base : « aujourd'hui » est fixé au 20/11/2026, délai cible de 15 jours. */
class AlerteServiceTest {

    DossierRepository dossiers = mock(DossierRepository.class);
    AlerteRepository alertes = mock(AlerteRepository.class);
    Clock horloge = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
    AlerteService service = new AlerteService(dossiers, alertes, horloge, 15);

    private Dossier dossierJugeLe(String date) {
        Dossier d = new Dossier();
        d.setId(1L);
        d.setDateJugement(LocalDate.parse(date));
        // La limite est aujourd'hui moins le délai cible : 20/11 − 15 jours = 05/11
        when(dossiers.findByEtapeAndDateJugementBeforeAndEleveArchiveLeIsNullOrderByDateJugementAsc(
                StatutDossier.JUGEMENT_RENDU, LocalDate.parse("2026-11-05"))).thenReturn(List.of(d));
        return d;
    }

    @Test // RG-11
    void une_alerte_au_plus_par_dossier() {
        dossierJugeLe("2026-10-01");
        when(alertes.existsByDossierId(1L)).thenReturn(false, true);

        assertThat(service.enregistrerNouvellesAlertes()).isEqualTo(1);
        assertThat(service.enregistrerNouvellesAlertes()).isZero();
        verify(alertes, times(1)).save(any());
    }

    @Test // RG-06 : le message parle de délai cible, pas de délai légal
    void le_message_parle_du_delai_cible() {
        dossierJugeLe("2026-10-01");
        when(alertes.existsByDossierId(1L)).thenReturn(false);

        service.enregistrerNouvellesAlertes();

        ArgumentCaptor<Alerte> alerte = ArgumentCaptor.forClass(Alerte.class);
        verify(alertes).save(alerte.capture());
        assertThat(alerte.getValue().getMessage())
                .isEqualTo("Jugement du 01/10/2026 non transcrit après le délai cible de 15 jours.");
        assertThat(alerte.getValue().getDateAlerte()).isEqualTo("2026-11-20T10:00");
    }
}
