package sn.samatur.samatur.eleve.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.commun.RessourceIntrouvableException;
import sn.samatur.samatur.eleve.dto.EleveDto;
import sn.samatur.samatur.eleve.repository.EleveRepository;

import java.util.List;

/** Lecture des élèves. Ils sont créés par l'import CSV (ImportCsvService), jamais un par un. */
@Service
public class EleveService {

    private final EleveRepository eleveRepository;

    public EleveService(EleveRepository eleveRepository) {
        this.eleveRepository = eleveRepository;
    }

    // Lister les élèves, en DTO construits dans la transaction (école et commune chargées à la demande)
    @Transactional(readOnly = true)
    public List<EleveDto> listerTous() {
        return eleveRepository.findAll(Sort.by("nom", "prenom")).stream().map(EleveDto::de).toList();
    }

    // Trouver un élève par son id
    @Transactional(readOnly = true)
    public EleveDto trouverParId(Long id) {
        return eleveRepository.findById(id).map(EleveDto::de)
                .orElseThrow(() -> new RessourceIntrouvableException("Élève " + id + " introuvable."));
    }
}
