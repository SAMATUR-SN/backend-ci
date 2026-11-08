package sn.samatur.samatur.security.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identifiant", nullable = false, unique = true)
    private String identifiant;

    @Column(name = "mot_de_passe_hash", nullable = false)
    private String motDePasseHash;

    @Column(name = "nom_affiche", nullable = false)
    private String nomAffiche;

    @Column(name = "role", nullable = false)
    private String role; // 'ECOLE', 'ETAT_CIVIL', 'TRIBUNAL', 'SUPERVISEUR'

    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Column(name = "ecole_id")
    private Long ecoleId;

    @Column(name = "centre_etat_civil_id")
    private Long centreEtatCivilId;

    @Column(name = "tribunal_instance_id")
    private Long tribunalInstanceId;

    // Constructeurs
    public Utilisateur() {}

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }

    public String getNomAffiche() { return nomAffiche; }
    public void setNomAffiche(String nomAffiche) { this.nomAffiche = nomAffiche; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Long getEcoleId() { return ecoleId; }
    public void setEcoleId(Long ecoleId) { this.ecoleId = ecoleId; }

    public Long getCentreEtatCivilId() { return centreEtatCivilId; }
    public void setCentreEtatCivilId(Long centreEtatCivilId) { this.centreEtatCivilId = centreEtatCivilId; }

    public Long getTribunalInstanceId() { return tribunalInstanceId; }
    public void setTribunalInstanceId(Long tribunalInstanceId) { this.tribunalInstanceId = tribunalInstanceId; }
}