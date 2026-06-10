package com.workbook_manager.workbook_manager.service;

import com.workbook_manager.workbook_manager.entite.Workbook;
import com.workbook_manager.workbook_manager.entite.Workplace;
import com.workbook_manager.workbook_manager.repository.WorkBookRepository;
import com.workbook_manager.workbook_manager.repository.WorkPlaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkPlaceService {

    // Injection du repository pour accéder aux données des postes de travail
    private final WorkPlaceRepository workplaceRepository;

    // Injection du repository pour accéder aux données des workbooks
    private final WorkBookRepository workbookRepository;

    // Lecture seule : retourne tous les postes d'un workbook triés par rang croissant
    @Transactional(readOnly = true)
    public List<Workplace> findByWorkbookId(Long workbookId) {
        return workplaceRepository.findByWorkbookIdOrderByRankAsc(workbookId);
    }

    // Lecture seule : récupère un poste par son identifiant
    @Transactional(readOnly = true)
    public Workplace findById(Long id) {
        return workplaceRepository.findById(id)
                // Lance une exception si aucun poste n'est trouvé avec cet id
                .orElseThrow(() -> new EntityNotFoundException("Workplace introuvable avec l'id : " + id));
    }

    /**
     * Ajoute un nouveau poste à un workbook.
     * Le rang est attribué automatiquement (max actuel + 1, commence à 1).
     * Si le poste est marqué comme actuel, les autres postes sont désactivés.
     */
    public Workplace addWorkplace(Long workbookId, Workplace workplace) {
        // Vérifie que le workbook existe avant d'ajouter le poste
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new EntityNotFoundException("Workbook introuvable avec l'id : " + workbookId));

        // Calcule le prochain rang : max existant + 1, ou 1 s'il n'y a aucun poste
        int nextRank = workplaceRepository.findMaxRankByWorkbookId(workbookId)
                .map(max -> max + 1)
                .orElse(1);

        // Associe le poste au workbook et lui attribue son rang
        workplace.setWorkbook(workbook);
        workplace.setRank(nextRank);

        // Si ce poste est marqué comme actuel, on retire le flag des autres postes
        if (workplace.isCurrent()) {
            clearCurrentFlag(workbookId);
        }

        // Sauvegarde et retourne le nouveau poste
        return workplaceRepository.save(workplace);
    }

    /**
     * Met à jour un poste existant.
     * Si le poste devient actuel, les autres postes sont désactivés.
     */
    public Workplace updateWorkplace(Long workplaceId, Workplace updated) {
        // Récupère le poste existant (lève une exception s'il n'existe pas)
        Workplace existing = findById(workplaceId);

        // Si le poste passe à "actuel" alors qu'il ne l'était pas, on nettoie les autres
        if (updated.isCurrent() && !existing.isCurrent()) {
            clearCurrentFlag(existing.getWorkbook().getId());
        }

        // Applique les nouvelles valeurs sur le poste existant
        existing.setCurrent(updated.isCurrent());
        existing.setCompanyCode(updated.getCompanyCode());
        existing.setCompanyName(updated.getCompanyName());
        existing.setCountryCode(updated.getCountryCode());
        existing.setCountryName(updated.getCountryName());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());

        // Sauvegarde et retourne le poste mis à jour
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

        // Supprime le poste de la base de données
        workplaceRepository.delete(workplace);

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

        // Échange les rangs : le poste qui occupait la position cible prend le rang actuel
        workplaceRepository.findByWorkbookIdAndRank(workbookId, targetRank)
                .ifPresent(other -> other.setRank(currentRank));

        // Attribue le rang cible au poste déplacé
        workplace.setRank(targetRank);
        workplaceRepository.save(workplace);
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

        // Échange les rangs : le poste qui occupait la position cible prend le rang actuel
        workplaceRepository.findByWorkbookIdAndRank(workbookId, targetRank)
                .ifPresent(other -> other.setRank(currentRank));

        // Attribue le rang cible au poste déplacé
        workplace.setRank(targetRank);
        workplaceRepository.save(workplace);
    }

    // Retire le flag "actuel" de tous les postes d'un workbook
    private void clearCurrentFlag(Long workbookId) {
        workplaceRepository.findByWorkbookIdOrderByRankAsc(workbookId)
                .forEach(wp -> wp.setCurrent(false));
    }
}