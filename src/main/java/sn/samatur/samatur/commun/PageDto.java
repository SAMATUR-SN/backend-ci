package sn.samatur.samatur.commun;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Une page de résultats, au format du contrat d'API :
 * {"contenu": [...], "page": 0, "taille": 20, "totalElements": 137, "totalPages": 7}.
 * Un DTO à nous plutôt que l'objet Page de Spring, dont le JSON n'est pas garanti stable.
 */
public record PageDto<T>(List<T> contenu, int page, int taille, long totalElements, int totalPages) {

    public static <T> PageDto<T> de(Page<T> p) {
        return new PageDto<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
