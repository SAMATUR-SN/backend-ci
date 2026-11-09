package sn.samatur.samatur.dossier.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import sn.samatur.samatur.commun.ConflitException;
import sn.samatur.samatur.commun.RegleMetierException;
import sn.samatur.samatur.commun.RessourceIntrouvableException;
import sn.samatur.samatur.dossier.dto.AvancerDossierRequest;
import sn.samatur.samatur.dossier.dto.TemoinDto;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.HistoriqueEtape;
import sn.samatur.samatur.dossier.entity.PreuveNaissance;
import sn.samatur.samatur.dossier.entity.StatutDossier;
import sn.samatur.samatur.dossier.entity.TypeAudience;
import sn.samatur.samatur.dossier.repository.DossierRepository;
import sn.samatur.samatur.dossier.repository.HistoriqueEtapeRepository;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.referentiel.entity.CentreEtatCivil;
import sn.samatur.samatur.referentiel.repository.CentreEtatCivilRepository;
import sn.samatur.samatur.referentiel.repository.TribunalInstanceRepository;
import sn.samatur.samatur.security.UserDetailsImpl;
import sn.samatur.samatur.security.entity.Utilisateur;
import sn.samatur.samatur.security.repository.UtilisateurRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Règles du cycle de vie, sans base ni serveur ; « aujourd'hui » est fixé au 10/11/2026. */
@ExtendWith(MockitoExtension.class)
class DossierServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 10);

    @Mock DossierRepository dossiers;
    @Mock HistoriqueEtapeRepository historique;
    @Mock CentreEtatCivilRepository centres;
    @Mock TribunalInstanceRepository tribunaux;
    @Mock UtilisateurRepository utilisateurs;

    Clock horloge = Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC);
    DossierService service;

    @BeforeEach
    void preparer() {
        service = new DossierService(dossiers, historique, centres, tribunaux, utilisateurs, horloge, 15);
        lenient().when(historique.findByDossierIdOrderByDateHeureAscIdAsc(any())).thenReturn(List.of());
        lenient().when(utilisateurs.findAllById(any())).thenReturn(List.of());
    }

    private Dossier dossier(StatutDossier etape) {
        Dossier d = new Dossier();
        d.setId(1L);
        d.setReference("ST-2026-000001");
        d.setEtape(etape);
        d.setDateDetection(AUJOURDHUI.minusDays(30));
        d.setEleve(new Eleve());
        when(dossiers.findById(1L)).thenReturn(Optional.of(d));
        return d;
    }

    private static UserDetailsImpl compte(long id, String role) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setIdentifiant(role.toLowerCase() + "@test.sn");
        u.setMotDePasseHash("x");
        u.setRole(role);
        u.setActif(true);
        return UserDetailsImpl.build(u);
    }

    private static AvancerDossierRequest demande(StatutDossier cible) {
        return new AvancerDossierRequest(cible, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    private static AvancerDossierRequest certificat(LocalDate date) {
        return new AvancerDossierRequest(StatutDossier.CERTIFICAT_OBTENU, "CERT-1", date, 1L, null, null,
                null, null, null, null, null, null, null, null, "certificat reçu");
    }

    private static AvancerDossierRequest jugement(LocalDate date) {
        return new AvancerDossierRequest(StatutDossier.JUGEMENT_RENDU, null, null, null, null, null,
                "1287/2026", date, 1L, TypeAudience.FORAINE, null, null, null, null, null);
    }

    @Test // RG-09
    void un_passage_normal_met_a_jour_le_dossier_et_ecrit_une_ligne_d_historique_complete() {
        Dossier d = dossier(StatutDossier.DETECTE);
        CentreEtatCivil centre = new CentreEtatCivil();
        centre.setId(1L);
        when(centres.findById(1L)).thenReturn(Optional.of(centre));

        var resultat = service.avancer(1L, certificat(AUJOURDHUI.minusDays(2)), compte(2, "ETAT_CIVIL"));

        assertThat(resultat.etape()).isEqualTo(StatutDossier.CERTIFICAT_OBTENU);
        assertThat(d.getNumeroCertificat()).isEqualTo("CERT-1");
        assertThat(d.getCentreEtatCivil()).isSameAs(centre);
        ArgumentCaptor<HistoriqueEtape> ligne = ArgumentCaptor.forClass(HistoriqueEtape.class);
        verify(historique).save(ligne.capture());
        assertThat(ligne.getValue().getEtapeDepart()).isEqualTo(StatutDossier.DETECTE);
        assertThat(ligne.getValue().getEtapeArrivee()).isEqualTo(StatutDossier.CERTIFICAT_OBTENU);
        assertThat(ligne.getValue().getAuteurId()).isEqualTo(2L);
        assertThat(ligne.getValue().getRoleAuteur()).isEqualTo("ETAT_CIVIL");
        assertThat(ligne.getValue().getDateHeure()).isEqualTo(LocalDateTime.of(2026, 11, 10, 10, 0));
        assertThat(ligne.getValue().getCommentaire()).isEqualTo("certificat reçu");
    }

    @Test // RG-02
    void un_saut_d_etape_est_refuse_en_422_et_rien_n_est_ecrit() {
        dossier(StatutDossier.DETECTE);

        assertThatThrownBy(() -> service.avancer(1L, demande(StatutDossier.JUGEMENT_RENDU), compte(3, "TRIBUNAL")))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("CERTIFICAT_OBTENU");
        verify(dossiers, never()).saveAndFlush(any());
        verify(historique, never()).save(any());
    }

    @Test // RG-02 : un double clic ne fait pas avancer deux fois
    void un_rejeu_est_refuse_en_409() {
        dossier(StatutDossier.CERTIFICAT_OBTENU);

        assertThatThrownBy(() -> service.avancer(1L, certificat(AUJOURDHUI), compte(2, "ETAT_CIVIL")))
                .isInstanceOf(ConflitException.class)
                .hasMessageContaining("déjà franchi");
        verify(historique, never()).save(any());
    }

    @Test // RG-02
    void un_dossier_termine_ne_peut_plus_avancer() {
        dossier(StatutDossier.ACTE_REMIS);

        assertThatThrownBy(() -> service.avancer(1L, demande(StatutDossier.ACTE_REMIS), compte(1, "ECOLE")))
                .isInstanceOf(ConflitException.class)
                .hasMessageContaining("terminé");
    }

    @Test // RG-03 : une école ne peut pas enregistrer un jugement
    void un_role_non_autorise_pour_l_etape_est_refuse_en_403() {
        dossier(StatutDossier.DOSSIER_COMPLET);

        assertThatThrownBy(() -> service.avancer(1L, jugement(AUJOURDHUI), compte(1, "ECOLE")))
                .isInstanceOf(AccessDeniedException.class);
        verify(historique, never()).save(any());
    }

    @Test // RG-03
    void un_champ_manquant_est_refuse_en_422_avec_la_liste_des_champs() {
        dossier(StatutDossier.DOSSIER_COMPLET);
        AvancerDossierRequest incomplete = new AvancerDossierRequest(StatutDossier.JUGEMENT_RENDU, null, null, null,
                null, null, "1287/2026", null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.avancer(1L, incomplete, compte(3, "TRIBUNAL")))
                .isInstanceOfSatisfying(RegleMetierException.class, e -> {
                    assertThat(e.code()).isEqualTo("champ-obligatoire");
                    assertThat(e.champs()).containsExactly("dateJugement", "tribunalId", "typeAudience");
                });
    }

    @Test // Cahier des charges, section 3
    void la_preuve_par_temoins_exige_exactement_deux_temoins_complets() {
        dossier(StatutDossier.CERTIFICAT_OBTENU);
        AvancerDossierRequest unSeul = new AvancerDossierRequest(StatutDossier.DOSSIER_COMPLET, null, null, null,
                PreuveNaissance.TEMOINS, List.of(new TemoinDto("Ibrahima Ndiaye", "CNI-1")),
                null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.avancer(1L, unSeul, compte(1, "ECOLE")))
                .isInstanceOfSatisfying(RegleMetierException.class,
                        e -> assertThat(e.champs()).containsExactly("temoins"));
    }

    @Test // RG-04
    void une_date_de_jugement_anterieure_au_certificat_est_refusee() {
        Dossier d = dossier(StatutDossier.DOSSIER_COMPLET);
        d.setDateCertificat(AUJOURDHUI.minusDays(10));
        when(tribunaux.findById(1L)).thenReturn(Optional.of(new sn.samatur.samatur.referentiel.entity.TribunalInstance()));

        assertThatThrownBy(() -> service.avancer(1L, jugement(AUJOURDHUI.minusDays(20)), compte(3, "TRIBUNAL")))
                .isInstanceOfSatisfying(RegleMetierException.class,
                        e -> assertThat(e.code()).isEqualTo("dates-incoherentes"));
        verify(historique, never()).save(any());
    }

    @Test // RG-04
    void une_date_dans_le_futur_est_refusee() {
        dossier(StatutDossier.DETECTE);
        when(centres.findById(1L)).thenReturn(Optional.of(new CentreEtatCivil()));

        assertThatThrownBy(() -> service.avancer(1L, certificat(AUJOURDHUI.plusDays(1)), compte(2, "ETAT_CIVIL")))
                .isInstanceOfSatisfying(RegleMetierException.class,
                        e -> assertThat(e.code()).isEqualTo("date-future"));
    }

    @Test // RG-05 : délai cible de 15 jours après le jugement
    void un_dossier_juge_il_y_a_15_jours_n_est_pas_en_retard_a_16_jours_il_l_est() {
        Dossier d = new Dossier();
        d.setEtape(StatutDossier.JUGEMENT_RENDU);

        d.setDateJugement(AUJOURDHUI.minusDays(15));
        assertThat(service.estEnRetard(d)).isFalse();

        d.setDateJugement(AUJOURDHUI.minusDays(16));
        assertThat(service.estEnRetard(d)).isTrue();

        d.setEtape(StatutDossier.TRANSCRIT);
        assertThat(service.estEnRetard(d)).isFalse();
    }

    @Test // RG-08, RG-09 : un dossier naît à DETECTE avec sa référence et sa première ligne d'historique
    void un_nouveau_dossier_recoit_une_reference_ST_AAAA_NNNNNN_et_un_historique() {
        when(dossiers.prochainNumero()).thenReturn(42L);

        Dossier d = service.creerDossierDetecte(new Eleve(), compte(1, "ECOLE"), "Dossier créé par import CSV");

        assertThat(d.getReference()).isEqualTo("ST-2026-000042");
        assertThat(d.getEtape()).isEqualTo(StatutDossier.DETECTE);
        assertThat(d.getDateDetection()).isEqualTo(AUJOURDHUI);
        ArgumentCaptor<HistoriqueEtape> ligne = ArgumentCaptor.forClass(HistoriqueEtape.class);
        verify(historique).save(ligne.capture());
        assertThat(ligne.getValue().getEtapeDepart()).isNull();
        assertThat(ligne.getValue().getEtapeArrivee()).isEqualTo(StatutDossier.DETECTE);
        assertThat(ligne.getValue().getRoleAuteur()).isEqualTo("ECOLE");
    }

    @Test // RG-03 : seule une école fait naître un dossier
    void seule_une_ecole_cree_un_dossier() {
        assertThatThrownBy(() -> service.creerDossierDetecte(new Eleve(), compte(2, "ETAT_CIVIL"), null))
                .isInstanceOf(AccessDeniedException.class);
        verify(dossiers, never()).save(any());
    }

    @Test
    void un_dossier_inexistant_donne_404() {
        when(dossiers.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.avancer(99L, demande(StatutDossier.CERTIFICAT_OBTENU), compte(2, "ETAT_CIVIL")))
                .isInstanceOf(RessourceIntrouvableException.class);
    }
}
