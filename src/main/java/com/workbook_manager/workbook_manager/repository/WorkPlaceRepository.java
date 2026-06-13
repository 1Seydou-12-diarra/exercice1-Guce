package com.workbook_manager.workbook_manager.repository;

import com.workbook_manager.workbook_manager.entite.Workplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface WorkPlaceRepository extends JpaRepository<Workplace, Long> {

    // Retourne tous les postes d'un workbook triés par rang croissant
    List<Workplace> findByWorkbookIdOrderByRankAsc(Long workbookId);

    // Recherche un poste précis dans un workbook par son rang
    Optional<Workplace> findByWorkbookIdAndRank(Long workbookId, Integer rank);

    // Compte le nombre total de postes appartenant à un workbook
    int countByWorkbookId(Long workbookId);

    // Récupère le rang le plus élevé parmi les postes d'un workbook
    @Query("SELECT MAX(wp.rank) FROM Workplace wp WHERE wp.workbook.id = :workbookId")
    Optional<Integer> findMaxRankByWorkbookId(@Param("workbookId") Long workbookId);

    // ✅ SQL natif avec ORDER BY DESC — évite le conflit de contrainte unique sur H2
    // H2 vérifie la contrainte ligne par ligne : en partant du rang le plus élevé,
    // aucune collision ne se produit pendant l'UPDATE
    @Modifying
    @Query(value = "UPDATE workplace SET rank = rank + 1 WHERE workbook_id = :workbookId AND rank >= :rank ORDER BY rank DESC", nativeQuery = true)
    void incrementRanksFrom(@Param("workbookId") Long workbookId, @Param("rank") Integer rank);

    // ✅ SQL natif avec ORDER BY ASC — évite le conflit de contrainte unique sur H2
    // En partant du rang le plus bas, aucune collision ne se produit pendant l'UPDATE
    @Modifying
    @Query(value = "UPDATE workplace SET rank = rank - 1 WHERE workbook_id = :workbookId AND rank > :rank ORDER BY rank ASC", nativeQuery = true)
    void decrementRanksAfter(@Param("workbookId") Long workbookId, @Param("rank") Integer rank);

    // Retire le flag "actuel" de tous les postes d'un workbook via UPDATE SQL direct
    @Modifying
    @Query("UPDATE Workplace wp SET wp.current = false WHERE wp.workbook.id = :workbookId")
    void clearCurrentFlagByWorkbookId(@Param("workbookId") Long workbookId);

    // Retourne les postes d'un workbook avec pagination, triés par rang croissant
    Page<Workplace> findByWorkbookIdOrderByRankAsc(Long workbookId, Pageable pageable);
}