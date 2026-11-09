package sn.samatur.samatur.eleve.entity;

import jakarta.persistence.*;
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

    @Column(nullable = false, length = 1)
    private String sexe;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "date_naissance_presumee")
    private Boolean dateNaissancePresumee;

    @Column(nullable = false, length = 20)
    private String classe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commune_naissance_id")
    private Commune communeNaissance;

    @Column(name = "nom_pere", length = 150)
    private String nomPere;

    @Column(name = "nom_mere", length = 150)
    private String nomMere;

    @Column(name = "telephone_parent", length = 13)
    private String telephoneParent;

    @Enumerated(EnumType.STRING)
    @Column(name = "langue_contact", nullable = false, length = 20)
    private Langue langueContact = Langue.FRANCAIS;
}