package sn.samatur.samatur.referentiel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.samatur.samatur.referentiel.entity.TribunalInstance;

@Repository
public interface TribunalInstanceRepository extends JpaRepository<TribunalInstance, Long> {
}
