package sn.samatur.samatur.dossier.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.StatutDossier;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long>, JpaSpecificationExecutor<Dossier> {

    /** Liste paginée et filtrée : l'élève, son école et sa commune sont chargés dans la même requête. */
    @Override
    @EntityGraph(attributePaths = {"eleve", "eleve.ecole", "eleve.communeNaissance"})
    Page<Dossier> findAll(Specification<Dossier> spec, Pageable pageable);

    /** Même chargement, sans pagination : liste des dossiers en retard (GET /api/alertes). */
    @Override
    @EntityGraph(attributePaths = {"eleve", "eleve.ecole", "eleve.communeNaissance"})
    List<Dossier> findAll(Specification<Dossier> spec, Sort sort);

    /** RG-05 : à cette étape et jugés avant la limite (aujourd'hui moins le délai cible). */
    List<Dossier> findByEtapeAndDateJugementBeforeAndEleveArchiveLeIsNullOrderByDateJugementAsc(StatutDossier etape, LocalDate limite);

    Optional<Dossier> findByEleveId(Long eleveId);

    boolean existsByEleveId(Long eleveId);

    /** Numéro suivant des références ST-AAAA-NNNNNN (RG-08). */
    @Query(value = "SELECT nextval('dossier_reference_seq')", nativeQuery = true)
    long prochainNumero();

    List<Dossier> findByEtape(StatutDossier etape);

    long countByEtape(StatutDossier etape);

    @Query("SELECT d.eleve.ecole.nom, COUNT(d) FROM Dossier d GROUP BY d.eleve.ecole.nom")
    List<Object[]> countDossiersParEcole();

    @Query("SELECT d.centreEtatCivil.nom, COUNT(d) FROM Dossier d GROUP BY d.centreEtatCivil.nom")
    List<Object[]> countDossiersParCentreEtatCivil();
}
