package sn.samatur.samatur.referentiel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "centre_etat_civil")
@Getter
@Setter
public class CentreEtatCivil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "commune_id")
    private Long communeId;
}