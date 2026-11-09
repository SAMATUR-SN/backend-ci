package sn.samatur.samatur.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.samatur.samatur.commun.RessourceIntrouvableException;
import sn.samatur.samatur.referentiel.entity.CentreEtatCivil;
import sn.samatur.samatur.referentiel.entity.Ecole;
import sn.samatur.samatur.referentiel.entity.TribunalInstance;
import sn.samatur.samatur.referentiel.repository.CentreEtatCivilRepository;
import sn.samatur.samatur.referentiel.repository.EcoleRepository;
import sn.samatur.samatur.referentiel.repository.TribunalInstanceRepository;
import sn.samatur.samatur.security.dto.UtilisateurDto;
import sn.samatur.samatur.security.entity.Utilisateur;
import sn.samatur.samatur.security.repository.UtilisateurRepository;

/** Construit le profil affiché de l'utilisateur connecté, avec le nom de sa structure. */
@Service
public class ProfilService {

    private final UtilisateurRepository utilisateurs;
    private final EcoleRepository ecoles;
    private final CentreEtatCivilRepository centres;
    private final TribunalInstanceRepository tribunaux;

    public ProfilService(UtilisateurRepository utilisateurs, EcoleRepository ecoles,
                         CentreEtatCivilRepository centres, TribunalInstanceRepository tribunaux) {
        this.utilisateurs = utilisateurs;
        this.ecoles = ecoles;
        this.centres = centres;
        this.tribunaux = tribunaux;
    }

    @Transactional(readOnly = true)
    public UtilisateurDto profil(String identifiant) {
        Utilisateur u = utilisateurs.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable."));
        return new UtilisateurDto(u.getIdentifiant(), u.getNomAffiche(), u.getRole(), structure(u));
    }

    private String structure(Utilisateur u) {
        if (u.getEcoleId() != null) {
            return ecoles.findById(u.getEcoleId()).map(Ecole::getNom).orElse(null);
        }
        if (u.getCentreEtatCivilId() != null) {
            return centres.findById(u.getCentreEtatCivilId()).map(CentreEtatCivil::getNom).orElse(null);
        }
        if (u.getTribunalInstanceId() != null) {
            return tribunaux.findById(u.getTribunalInstanceId()).map(TribunalInstance::getNom).orElse(null);
        }
        return null;
    }
}
