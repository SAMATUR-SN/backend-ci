package sn.samatur.samatur.eleve.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.eleve.repository.EleveRepository;
import sn.samatur.samatur.referentiel.entity.Commune;
import sn.samatur.samatur.referentiel.entity.Ecole;
import sn.samatur.samatur.referentiel.repository.CommuneRepository;
import sn.samatur.samatur.referentiel.repository.EcoleRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EleveServiceTest {

    @Mock
    private EleveRepository eleveRepository;

    @Mock
    private EcoleRepository ecoleRepository;

    @Mock
    private CommuneRepository communeRepository;

    @InjectMocks
    private EleveService eleveService;

    @Test
    void importsTheRequiredBirthCommune() {
        Ecole ecole = new Ecole();
        ecole.setId(1L);
        Commune commune = new Commune();
        commune.setId(2L);
        when(ecoleRepository.findById(1L)).thenReturn(Optional.of(ecole));
        when(communeRepository.findById(2L)).thenReturn(Optional.of(commune));
        when(eleveRepository.findByNumeroIne("INE-1")).thenReturn(Optional.empty());
        when(eleveRepository.save(any(Eleve.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile csv = new MockMultipartFile(
                "file",
                "eleves.csv",
                "text/csv",
                "numero_ine;nom;prenom;classe;ecole_id;date_naissance;sexe;commune_naissance_id\n"
                        .concat("INE-1;Diop;Awa;CM2;1;2014-01-15;F;2\n")
                        .getBytes(StandardCharsets.UTF_8));

        assertTrue(eleveService.importerCsv(csv).isEmpty());

        ArgumentCaptor<Eleve> eleveCaptor = ArgumentCaptor.forClass(Eleve.class);
        verify(eleveRepository).save(eleveCaptor.capture());
        assertEquals(2L, eleveCaptor.getValue().getCommuneNaissance().getId());
    }
}
