package sn.samatur.samatur.eleve.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import sn.samatur.samatur.referentiel.entity.Commune;
import sn.samatur.samatur.referentiel.entity.Ecole;

import java.time.LocalDate;

@Entity
@Table(name = "eleve")
@Getter
@Setter
public class Eleve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ine", unique = true)
    private String numeroIne;

    // Ajoute cette ligne pour le sexe :
    @Column(nullable = false, length = 10)
    private String sexe;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @NotNull
    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "date_naissance_presumee")
    private Boolean dateNaissancePresumee;

    @Column(nullable = false, length = 20)
    private String classe;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "commune_naissance_id", nullable = false)
    private Commune communeNaissance;

    @Column(name = "nom_pere", length = 150)
    private String nomPere;

    @Column(name = "profession_pere", length = 100)
    private String professionPere;

    @Column(name = "nom_mere", length = 150)
    private String nomMere;

    @Column(name = "profession_mere", length = 100)
    private String professionMere;
}