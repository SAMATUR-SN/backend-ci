package sn.samatur.samatur.dossier.repository;

import org.springframework.data.jpa.domain.Specification;
import sn.samatur.samatur.dossier.entity.Dossier;
import sn.samatur.samatur.dossier.entity.StatutDossier;

import java.time.LocalDate;

/** Un filtre par critère de la liste des dossiers ; chacun ne filtre rien si son paramètre est absent. */
public final class DossierSpecifications {

    private DossierSpecifications() {
    }

    public static Specification<Dossier> etape(StatutDossier etape) {
        return (r, q, cb) -> etape == null ? null : cb.equal(r.get("etape"), etape);
    }

    public static Specification<Dossier> commune(Long communeId) {
        return (r, q, cb) -> communeId == null ? null
                : cb.equal(r.get("eleve").get("communeNaissance").get("id"), communeId);
    }

    public static Specification<Dossier> ecole(Long ecoleId) {
        return (r, q, cb) -> ecoleId == null ? null : cb.equal(r.get("eleve").get("ecole").get("id"), ecoleId);
    }

    public static Specification<Dossier> classe(String classe) {
        return (r, q, cb) -> classe == null || classe.isBlank() ? null
                : cb.equal(r.get("eleve").get("classe"), classe.trim().toUpperCase());
    }

    /**
     * RG-05 en requête : à JUGEMENT_RENDU et jugé avant la limite (aujourd'hui moins le délai cible).
     * enRetard=false renvoie tous les autres dossiers.
     */
    public static Specification<Dossier> enRetard(Boolean filtre, LocalDate limite) {
        return (r, q, cb) -> {
            if (filtre == null) {
                return null;
            }
            var retard = cb.and(cb.equal(r.get("etape"), StatutDossier.JUGEMENT_RENDU),
                    cb.lessThan(r.<LocalDate>get("dateJugement"), limite));
            return filtre ? retard : cb.not(retard);
        };
    }

    /** Recherche sans casse dans la référence, le nom et le prénom de l'élève. */
    public static Specification<Dossier> recherche(String texte) {
        return (r, q, cb) -> {
            if (texte == null || texte.isBlank()) {
                return null;
            }
            String motif = "%" + texte.trim().toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            return cb.or(cb.like(cb.lower(r.get("reference")), motif, '\\'),
                    cb.like(cb.lower(r.get("eleve").get("nom")), motif, '\\'),
                    cb.like(cb.lower(r.get("eleve").get("prenom")), motif, '\\'));
        };
    }
}
