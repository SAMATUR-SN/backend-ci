package sn.samatur.samatur.referentiel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.samatur.samatur.referentiel.entity.Commune;

@Repository
public interface CommuneRepository extends JpaRepository<Commune, Long> {
}