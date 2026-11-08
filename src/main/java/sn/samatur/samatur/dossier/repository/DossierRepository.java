package sn.samatur.samatur.dossier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.StatutDossier;

import java.util.List;
import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {

    Optional<Dossier> findByEleveId(Long eleveId);
    boolean existsByReference(String reference);
    List<Dossier> findByEtape(StatutDossier etape);

    long countByEtape(StatutDossier etape);

    @Query("SELECT d.eleve.ecole.nom, COUNT(d) FROM Dossier d GROUP BY d.eleve.ecole.nom")
    List<Object[]> countDossiersParEcole();

    @Query("SELECT d.centreEtatCivil.nom, COUNT(d) FROM Dossier d GROUP BY d.centreEtatCivil.nom")
    List<Object[]> countDossiersParCentreEtatCivil();
}
