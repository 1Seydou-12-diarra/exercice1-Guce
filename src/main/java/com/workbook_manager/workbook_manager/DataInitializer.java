package com.workbook_manager.workbook_manager;

import com.workbook_manager.workbook_manager.entite.Workbook;
import com.workbook_manager.workbook_manager.entite.Workplace;
import com.workbook_manager.workbook_manager.repository.WorkBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Composant Spring chargé d'initialiser des données de test au démarrage de l'application.
 * Implémente CommandLineRunner pour s'exécuter automatiquement après le lancement du contexte Spring.
 */
@Component
@RequiredArgsConstructor // Génère un constructeur avec les champs 'final' (injection de dépendances)
public class DataInitializer implements CommandLineRunner {

    // Référentiel JPA pour persister les entités Workbook en base de données
    private final WorkBookRepository workbookRepository;

    /**
     * Méthode exécutée automatiquement au démarrage de l'application.
     * Crée et sauvegarde un Workbook (livret de travail) avec ses Workplaces (expériences professionnelles).
     *
     * @param args Arguments de la ligne de commande (non utilisés ici)
     */
    @Override
    public void run(String... args) {

        // --- Création du Workbook (profil de l'employé) n°1 ---
        Workbook wb1 = new Workbook();
        wb1.setFirstName("Alice");                          // Prénom
        wb1.setLastName("Martin");                          // Nom de famille
        wb1.setBirthdate(LocalDate.of(1990, 5, 14));        // Date de naissance : 14 mai 1990
        wb1.setPassportNumber("FR1234567");                 // Numéro de passeport
        wb1.setEmail("alice.martin@example.com");           // Adresse e-mail

        // --- Première expérience professionnelle (terminée) ---
        Workplace wp1 = new Workplace();
        wp1.setCompanyCode("ACME");                         // Code interne de l'entreprise
        wp1.setCompanyName("Acme Corporation");             // Nom complet de l'entreprise
        wp1.setCountryCode("FR");                           // Code pays ISO
        wp1.setCountryName("France");                       // Nom du pays
        wp1.setStartDate(LocalDate.of(2015, 1, 1));         // Date de début : 1er janvier 2015
        wp1.setEndDate(LocalDate.of(2019, 12, 31));         // Date de fin : 31 décembre 2019
        wp1.setCurrent(false);                              // Poste non actuel (terminé)
        wp1.setRank(1);                                     // Ordre chronologique : 1ère expérience
        wp1.setWorkbook(wb1);                               // Lien vers le Workbook parent

        // --- Deuxième expérience professionnelle (en cours) ---
        Workplace wp2 = new Workplace();
        wp2.setCompanyCode("TECH");                         // Code interne de l'entreprise
        wp2.setCompanyName("TechSolutions SAS");            // Nom complet de l'entreprise
        wp2.setCountryCode("FR");                           // Code pays ISO
        wp2.setCountryName("France");                       // Nom du pays
        wp2.setStartDate(LocalDate.of(2020, 1, 15));        // Date de début : 15 janvier 2020
        wp2.setEndDate(null);                               // Pas de date de fin (poste actuel)
        wp2.setCurrent(true);                               // Poste actuel (en cours)
        wp2.setRank(2);                                     // Ordre chronologique : 2ème expérience
        wp2.setWorkbook(wb1);                               // Lien vers le Workbook parent

        // --- Association des expériences au Workbook ---
        wb1.getWorkplaces().add(wp1);   // Ajout de la 1ère expérience à la liste
        wb1.getWorkplaces().add(wp2);   // Ajout de la 2ème expérience à la liste

        // Sauvegarde du Workbook en base de données (les Workplaces sont sauvegardés en cascade)
        workbookRepository.save(wb1);
    }
}