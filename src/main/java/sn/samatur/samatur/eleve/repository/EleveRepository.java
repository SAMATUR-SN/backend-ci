package sn.samatur.samatur.eleve.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.samatur.samatur.eleve.entity.Eleve;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface EleveRepository extends JpaRepository<Eleve, Long> {
    Optional<Eleve> findByNumeroIne(String numeroIne);

    boolean existsByNumeroIne(String numeroIne);

    /** RG-07 : même prénom, nom, date de naissance et école (sans tenir compte de la casse). */
    boolean existsByPrenomIgnoreCaseAndNomIgnoreCaseAndDateNaissanceAndEcoleId(
            String prenom, String nom, LocalDate dateNaissance, Long ecoleId);
}