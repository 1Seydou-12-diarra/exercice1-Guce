package com.workbook_manager.workbook_manager.repository;

import com.workbook_manager.workbook_manager.entite.Workplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkPlaceRepository extends JpaRepository<Workplace, Long> {

    // Retourne tous les postes d'un workbook triés par rang croissant (1, 2, 3...)
    // Utilisé pour afficher les postes dans le bon ordre
    List<Workplace> findByWorkbookIdOrderByRankAsc(Long workbookId);

    // Recherche un poste précis dans un workbook par son rang
    // Utilisé lors des déplacements (monter/descendre) pour trouver le poste voisin
    Optional<Workplace> findByWorkbookIdAndRank(Long workbookId, Integer rank);

    // Compte le nombre total de postes appartenant à un workbook
    // Utilisé pour afficher le compteur de postes sur la liste
    int countByWorkbookId(Long workbookId);

    // Requête JPQL : récupère le rang le plus élevé parmi les postes d'un workbook
    // Utilisé lors de l'ajout d'un poste pour calculer le prochain rang (max + 1)
    // Retourne Optional.empty() si le workbook n'a encore aucun poste
    @Query("SELECT MAX(wp.rank) FROM Workplace wp WHERE wp.workbook.id = :workbookId")
    Optional<Integer> findMaxRankByWorkbookId(@Param("workbookId") Long workbookId);

    // Requête JPQL de mise à jour : décrémente de 1 le rang de tous les postes
    // situés APRÈS le rang supprimé, afin de combler le "trou" laissé par la suppression
    // Exemple : suppression du rang 2 → les rangs 3, 4, 5 deviennent 2, 3, 4
    // @Modifying indique que cette requête modifie des données (UPDATE)
    @Modifying
    @Query("UPDATE Workplace wp SET wp.rank = wp.rank - 1 WHERE wp.workbook.id = :workbookId AND wp.rank > :rank")
    void decrementRanksAfter(@Param("workbookId") Long workbookId, @Param("rank") Integer rank);

    // Requête JPQL de mise à jour : incrémente de 1 le rang de tous les postes
    // à partir d'un rang donné, pour libérer une place lors d'une insertion
    // Exemple : insertion au rang 2 → les rangs 2, 3, 4 deviennent 3, 4, 5
    // @Modifying indique que cette requête modifie des données (UPDATE)
    @Modifying
    @Query("UPDATE Workplace wp SET wp.rank = wp.rank + 1 WHERE wp.workbook.id = :workbookId AND wp.rank >= :rank")
    void incrementRanksFrom(@Param("workbookId") Long workbookId, @Param("rank") Integer rank);
}