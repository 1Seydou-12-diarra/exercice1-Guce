package com.workbook_manager.workbook_manager.repository;

import com.workbook_manager.workbook_manager.entite.Workbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkBookRepository extends JpaRepository<Workbook, Long> {

    // Recherche un workbook par son numéro de passeport
    Optional<Workbook> findByPassportNumber(String passportNumber);

    // Recherche un workbook par son adresse email
    Optional<Workbook> findByEmail(String email);

    // Vérifie si un workbook existe déjà avec ce numéro de passeport
    boolean existsByPassportNumber(String passportNumber);

    // Vérifie si un workbook existe déjà avec cet email
    boolean existsByEmail(String email);

    // Vérifie si un AUTRE workbook (id différent) utilise déjà ce numéro de passeport
    boolean existsByPassportNumberAndIdNot(String passportNumber, Long id);

    // Vérifie si un AUTRE workbook (id différent) utilise déjà cet email
    boolean existsByEmailAndIdNot(String email, Long id);

    // Requête personnalisée JPQL : retourne tous les workbooks
    // triés par nom de famille puis par prénom, tous les deux en ordre alphabétique
    @Query("SELECT w FROM Workbook w ORDER BY w.lastName ASC, w.firstName ASC")
    List<Workbook> findAllOrderByName();

}
