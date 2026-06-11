package com.workbook_manager.workbook_manager.service;

import com.workbook_manager.workbook_manager.entite.Workbook;
import com.workbook_manager.workbook_manager.entite.Workplace;
import com.workbook_manager.workbook_manager.repository.WorkBookRepository;
import com.workbook_manager.workbook_manager.repository.WorkPlaceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkPlaceService {

    private final WorkPlaceRepository workplaceRepository;
    private final WorkBookRepository workbookRepository;
    // EntityManager nécessaire pour vider le cache Hibernate après les requêtes JPQL natives
    private final EntityManager entityManager;

    // Lecture seule : retourne tous les postes d'un workbook triés par rang croissant
    @Transactional(readOnly = true)
    public List<Workplace> findByWorkbookId(Long workbookId) {
        return workplaceRepository.findByWorkbookIdOrderByRankAsc(workbookId);
    }

    // Lecture seule : récupère un poste par son identifiant
    @Transactional(readOnly = true)
    public Workplace findById(Long id) {
        return workplaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workplace introuvable avec l'id : " + id));
    }

    /**
     * Ajoute un nouveau poste à un workbook.
     * Insère toujours en rang 1 et décale les postes existants via une requête SQL directe.
     */
    public Workplace addWorkplace(Long workbookId, Workplace workplace) {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new EntityNotFoundException("Workbook introuvable avec l'id : " + workbookId));

        // ✅ Force la création d'un nouvel enregistrement
        // Evite qu'un id parasite provoque un UPDATE au lieu d'un INSERT
        workplace.setId(null);

        if (workplace.isCurrent()) {
            clearCurrentFlag(workbookId);
        }

        workplaceRepository.incrementRanksFrom(workbookId, 1);

        entityManager.flush();
        entityManager.clear();

        workplace.setWorkbook(workbook);
        workplace.setRank(1);
        return workplaceRepository.save(workplace);
    }

    /**
     * Met à jour un poste existant.
     * Si le poste devient actuel, les autres postes sont désactivés.
     */
    public Workplace updateWorkplace(Long workplaceId, Workplace updated) {
        // Récupère le poste existant
        Workplace existing = findById(workplaceId);

        // Si le poste passe à "actuel" alors qu'il ne l'était pas, nettoie les autres
        if (updated.isCurrent() && !existing.isCurrent()) {
            clearCurrentFlag(existing.getWorkbook().getId());
            entityManager.flush();
            entityManager.clear();
            // Recharge le poste après le clear du cache
            existing = findById(workplaceId);
        }

        // Applique les nouvelles valeurs sur le poste existant
        existing.setCurrent(updated.isCurrent());
        existing.setCompanyCode(updated.getCompanyCode());
        existing.setCompanyName(updated.getCompanyName());
        existing.setCountryCode(updated.getCountryCode());
        existing.setCountryName(updated.getCountryName());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());

        return workplaceRepository.save(existing);
    }

    /**
     * Supprime un poste et réajuste les rangs des postes restants.
     */
    public void deleteWorkplace(Long workplaceId) {
        // Récupère le poste à supprimer
        Workplace workplace = findById(workplaceId);
        Long workbookId = workplace.getWorkbook().getId();
        int deletedRank = workplace.getRank();

        // Supprime le poste et flush avant de réajuster les rangs
        workplaceRepository.delete(workplace);
        entityManager.flush();

        // Décrémente de 1 le rang de tous les postes situés après celui supprimé
        workplaceRepository.decrementRanksAfter(workbookId, deletedRank);
    }

    /**
     * Remonte un poste d'une position (rang diminué de 1).
     */
    public void moveUp(Long workplaceId) {
        Workplace workplace = findById(workplaceId);

        // Impossible de monter si le poste est déjà en première position
        if (workplace.getRank() <= 1) return;

        Long workbookId = workplace.getWorkbook().getId();
        int currentRank = workplace.getRank();
        int targetRank = currentRank - 1;

        // Étape 1 : rang temporaire sur le poste qui occupe la position cible
        // Évite le conflit de contrainte unique (workbook_id, rank) lors de l'échange
        workplaceRepository.findByWorkbookIdAndRank(workbookId, targetRank)
                .ifPresent(other -> {
                    other.setRank(-1); // rang temporaire pour libérer la position cible
                    workplaceRepository.saveAndFlush(other);
                });

        // Étape 2 : déplace le poste courant vers la position cible
        workplace.setRank(targetRank);
        workplaceRepository.saveAndFlush(workplace);

        // Étape 3 : attribue l'ancien rang au poste déplacé temporairement
        workplaceRepository.findByWorkbookIdAndRank(workbookId, -1)
                .ifPresent(other -> {
                    other.setRank(currentRank);
                    workplaceRepository.save(other);
                });
    }

    /**
     * Descend un poste d'une position (rang augmenté de 1).
     */
    public void moveDown(Long workplaceId) {
        Workplace workplace = findById(workplaceId);
        Long workbookId = workplace.getWorkbook().getId();
        int currentRank = workplace.getRank();

        // Récupère le rang maximum pour vérifier si le poste est déjà en dernière position
        int maxRank = workplaceRepository.findMaxRankByWorkbookId(workbookId).orElse(1);

        // Impossible de descendre si le poste est déjà en dernière position
        if (currentRank >= maxRank) return;

        int targetRank = currentRank + 1;

        // Étape 1 : rang temporaire sur le poste qui occupe la position cible
        // Évite le conflit de contrainte unique (workbook_id, rank) lors de l'échange
        workplaceRepository.findByWorkbookIdAndRank(workbookId, targetRank)
                .ifPresent(other -> {
                    other.setRank(-1); // rang temporaire pour libérer la position cible
                    workplaceRepository.saveAndFlush(other);
                });

        // Étape 2 : déplace le poste courant vers la position cible
        workplace.setRank(targetRank);
        workplaceRepository.saveAndFlush(workplace);

        // Étape 3 : attribue l'ancien rang au poste déplacé temporairement
        workplaceRepository.findByWorkbookIdAndRank(workbookId, -1)
                .ifPresent(other -> {
                    other.setRank(currentRank);
                    workplaceRepository.save(other);
                });
    }

    /**
     * Retire le flag "actuel" de tous les postes d'un workbook via UPDATE SQL direct.
     * N'utilise pas le cache Hibernate pour garantir la cohérence en BDD.
     */
    private void clearCurrentFlag(Long workbookId) {
        // UPDATE SQL direct — plus fiable que de modifier les entités en mémoire
        workplaceRepository.clearCurrentFlagByWorkbookId(workbookId);
    }
}