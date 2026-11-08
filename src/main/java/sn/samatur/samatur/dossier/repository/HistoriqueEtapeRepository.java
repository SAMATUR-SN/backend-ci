package sn.samatur.samatur.dossier.repository;

import org.springframework.data.jpa.repository.JpaRepository; // Correction : JpaRepository
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.samatur.samatur.dossier.entity.HistoriqueEtape;

import java.util.List;

@Repository
public interface HistoriqueEtapeRepository extends JpaRepository<HistoriqueEtape, Long> {
    List<HistoriqueEtape> findByDossierId(Long dossierId);
}
