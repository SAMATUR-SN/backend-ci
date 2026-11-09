package sn.samatur.samatur.dossier.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Témoin de la naissance : deux exactement quand la preuve de naissance est TEMOINS. */
@Entity
@Table(name = "temoin")
@Getter
@Setter
public class Temoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Pas de sérialisation du lien retour : sinon dossier → témoins → dossier… à l'infini
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private Dossier dossier;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    @Column(name = "numero_piece")
    private String numeroPiece;
}
