package sn.samatur.samatur.dossier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import sn.samatur.samatur.eleve.entity.Eleve;
import sn.samatur.samatur.referentiel.entity.CentreEtatCivil;
import sn.samatur.samatur.referentiel.entity.TribunalInstance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "dossier")
@Getter
@Setter
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @OneToOne
    @JoinColumn(name = "eleve_id", nullable = false, unique = true)
    private Eleve eleve;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDossier etape = StatutDossier.DETECTE;

    @Column(name = "date_detection")
    private LocalDate dateDetection;

    @Column(name = "numero_certificat")
    private String numeroCertificat;

    @Column(name = "date_certificat")
    private LocalDate dateCertificat;

    @Enumerated(EnumType.STRING)
    @Column(name = "preuve_naissance")
    private PreuveNaissance preuveNaissance;

    @Column(name = "numero_jugement")
    private String numeroJugement;

    @Column(name = "date_jugement")
    private LocalDate dateJugement;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_audience")
    private TypeAudience typeAudience;

    @Column(name = "numero_acte")
    private String numeroActe;

    @Column(name = "annee_acte")
    private Integer anneeActe;

    @Column(name = "date_transcription")
    private LocalDate dateTranscription;

    @Column(name = "date_remise")
    private LocalDate dateRemise;

    @ManyToOne
    @JoinColumn(name = "centre_etat_civil_id")
    private CentreEtatCivil centreEtatCivil;

    @ManyToOne
    @JoinColumn(name = "tribunal_instance_id")
    private TribunalInstance tribunalInstance;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Temoin> temoins = new ArrayList<>();

    @Version
    private Long version;
}