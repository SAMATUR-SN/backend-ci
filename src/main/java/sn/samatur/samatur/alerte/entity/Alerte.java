package sn.samatur.samatur.alerte.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import sn.samatur.samatur.dossier.entity.Dossier;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerte")
@Getter
@Setter
public class Alerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dossier_id", nullable = false)
    private Dossier dossier;

    @Column(nullable = false)
    private String message;

    @Column(name = "date_alerte", nullable = false)
    private LocalDateTime dateAlerte;

    @Column(nullable = false)
    private boolean resolue = false;
}
