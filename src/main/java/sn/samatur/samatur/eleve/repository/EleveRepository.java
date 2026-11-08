package sn.samatur.samatur.eleve.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.samatur.samatur.eleve.entity.Eleve;

import java.util.Optional;

@Repository
public interface EleveRepository extends JpaRepository<Eleve, Long> {
    Optional<Eleve> findByNumeroIne(String numeroIne);
}