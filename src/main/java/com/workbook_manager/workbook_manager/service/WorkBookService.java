package com.workbook_manager.workbook_manager.service;


import com.workbook_manager.workbook_manager.entite.Workbook;
import com.workbook_manager.workbook_manager.repository.WorkBookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkBookService {

    // Injection du repository pour accéder aux données des workbooks
    private final WorkBookRepository  workbookRepository;

    // Lecture seule : optimise la transaction (pas de flush ni de dirty checking)
    @Transactional(readOnly = true)
    public List<Workbook> findAll() {
        // Retourne tous les workbooks triés par nom via une requête personnalisée
        return workbookRepository.findAllOrderByName();
    }

    // Lecture seule : récupère un workbook par son identifiant
    @Transactional(readOnly = true)
    public Workbook findById(Long id) {
        return workbookRepository.findById(id)
                // Lance une exception si aucun workbook n'est trouvé avec cet id
                .orElseThrow(() -> new EntityNotFoundException("Workbook introuvable avec "));
    }

    // Crée et enregistre un nouveau workbook en base
    public Workbook save(Workbook workbook) {
        // Vérifie que le passeport et l'email ne sont pas déjà utilisés
        validateUniqueness(workbook);
        return workbookRepository.save(workbook);
    }

    // Met à jour un workbook existant identifié par son id
    public Workbook update(Long id, Workbook updated) {
        // Récupère le workbook existant (lève une exception s'il n'existe pas)
        Workbook existing = findById(id);

        // Vérifie l'unicité en excluant le workbook lui-même de la comparaison
        validateUniquenessForUpdate(updated, id);

        // Applique les nouvelles valeurs sur l'entité existante
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setBirthdate(updated.getBirthdate());
        existing.setPassportNumber(updated.getPassportNumber());
        existing.setEmail(updated.getEmail());

        // Sauvegarde et retourne le workbook mis à jour
        return workbookRepository.save(existing);
    }

    // Supprime un workbook par son id
    public void deleteById(Long id) {
        // Vérifie que le workbook existe avant de tenter la suppression
        if (!workbookRepository.existsById(id)) {
            throw new EntityNotFoundException("Workbook introuvable " );
        }
        workbookRepository.deleteById(id);
    }

    // Vérifie qu'aucun autre workbook n'utilise déjà ce passeport ou cet email (création)
    private void validateUniqueness(Workbook workbook) {
        if (workbookRepository.existsByPassportNumber(workbook.getPassportNumber())) {
            throw new IllegalArgumentException("Ce numéro de passeport est déjà utilisé.");
        }
        if (workbookRepository.existsByEmail(workbook.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
    }

    // Vérifie l'unicité lors d'une mise à jour en ignorant le workbook en cours de modification
    private void validateUniquenessForUpdate(Workbook workbook, Long id) {
        // Cherche un autre workbook (id différent) ayant le même numéro de passeport
        if (workbookRepository.existsByPassportNumberAndIdNot(workbook.getPassportNumber(), id)) {
            throw new IllegalArgumentException("Ce numéro de passeport est déjà utilisé.");
        }
        // Cherche un autre workbook (id différent) ayant le même email
        if (workbookRepository.existsByEmailAndIdNot(workbook.getEmail(), id)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
    }
}
