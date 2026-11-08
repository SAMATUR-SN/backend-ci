package sn.samatur.samatur.dossier.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.HistoriqueEtape;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.repository.DossierRepository;
import sn.samatur.samatur.dossier.repository.HistoriqueEtapeRepository;
import sn.samatur.samatur.eleve.repository.EleveRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierServiceTest {

    @Mock
    private DossierRepository dossierRepository;

    @Mock
    private HistoriqueEtapeRepository historiqueEtapeRepository;

    @Mock
    private EleveRepository eleveRepository;

    @InjectMocks
    private DossierService dossierService;

    @Test
    void advancesToTheNextStatusAndRecordsTheTransition() {
        Dossier dossier = new Dossier();
        dossier.setId(7L);
        dossier.setEtape(StatutDossier.DETECTE);
        when(dossierRepository.findById(7L)).thenReturn(Optional.of(dossier));
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historiqueEtapeRepository.save(any(HistoriqueEtape.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Dossier updated = dossierService.avancerEtape(7L);

        assertEquals(StatutDossier.CERTIFICAT_ETABLI, updated.getEtape());
        ArgumentCaptor<HistoriqueEtape> historyCaptor = ArgumentCaptor.forClass(HistoriqueEtape.class);
        verify(historiqueEtapeRepository).save(historyCaptor.capture());
        assertEquals(StatutDossier.DETECTE, historyCaptor.getValue().getEtapeDepart());
        assertEquals(StatutDossier.CERTIFICAT_ETABLI, historyCaptor.getValue().getEtapeArrivee());
    }

    @Test
    void doesNotAdvanceACompletedDossier() {
        Dossier dossier = new Dossier();
        dossier.setEtape(StatutDossier.REMIS);
        when(dossierRepository.findById(7L)).thenReturn(Optional.of(dossier));

        assertThrows(IllegalStateException.class, () -> dossierService.avancerEtape(7L));
    }
}
