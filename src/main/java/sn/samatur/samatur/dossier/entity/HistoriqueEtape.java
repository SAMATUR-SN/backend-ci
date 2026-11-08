package sn.samatur.samatur.dossier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_etape")
@Getter
@Setter
public class HistoriqueEtape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dossier_id", nullable = false)
    private Dossier dossier;

    @Enumerated(EnumType.STRING)
    @Column(name = "etape_arrivee", nullable = false)
    private StatutDossier etapeArrivee;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Column(name = "auteur_id")
    private Long auteurId;

    @Column(name = "role_auteur")
    private String roleAuteur;

    private String commentaire;
}