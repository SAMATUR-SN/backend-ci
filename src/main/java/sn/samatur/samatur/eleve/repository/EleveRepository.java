package sn.samatur.samatur.eleve.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.samatur.samatur.eleve.entity.Eleve;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface EleveRepository extends JpaRepository<Eleve, Long> {
    Optional<Eleve> findByNumeroIne(String numeroIne);

    /** Le numéro INE est unique en base, élèves archivés compris. */
    boolean existsByNumeroIne(String numeroIne);

    /** RG-07 : même prénom, nom, date de naissance et école parmi les élèves actifs (sans tenir compte de la casse). */
    boolean existsByPrenomIgnoreCaseAndNomIgnoreCaseAndDateNaissanceAndEcoleIdAndArchiveLeIsNull(
            String prenom, String nom, LocalDate dateNaissance, Long ecoleId);

    /** RG-07 lors d'une modification : un autre élève actif que celui qu'on modifie. */
    boolean existsByPrenomIgnoreCaseAndNomIgnoreCaseAndDateNaissanceAndEcoleIdAndArchiveLeIsNullAndIdNot(
            String prenom, String nom, LocalDate dateNaissance, Long ecoleId, Long id);

    /** Les élèves actifs d'une école, avec leur école et leur commune chargées dans la même requête. */
    @EntityGraph(attributePaths = {"ecole", "communeNaissance"})
    Page<Eleve> findByEcoleIdAndArchiveLeIsNull(Long ecoleId, Pageable pageable);

    /** Un élève actif de l'école donnée : un élève d'une autre école reste introuvable (404). */
    Optional<Eleve> findByIdAndEcoleIdAndArchiveLeIsNull(Long id, Long ecoleId);
}
