package sn.samatur.samatur.alerte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.samatur.samatur.alerte.entity.Alerte;

import java.util.List;

@Repository
public interface AlerteRepository extends JpaRepository<Alerte, Long> {
    List<Alerte> findByResolueFalse();
    boolean existsByDossierId(Long dossierId);
}
