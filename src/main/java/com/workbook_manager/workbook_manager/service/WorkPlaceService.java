package com.workbook_manager.workbook_manager.service;

import com.workbook_manager.workbook_manager.dto.WorkplaceDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service metier gerant les operations sur les Workplaces.
 * Un Workplace est toujours rattache a un Workbook parent.
 * La gestion du rang garantit que les postes sont toujours
 * consecutifs et commencent a 1.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkPlaceService {

    private final WorkPlaceRepository workplaceRepository;
    private final WorkBookRepository workbookRepository;
    private final EntityManager entityManager;

    /**
     * Retourne tous les postes d'un workbook tries par rang croissant.
     * Utilise pour les listes sans pagination.
     */
    public List<WorkplaceDto> findByWorkbookId(Long workbookId) {
        return workplaceRepository.findByWorkbookIdOrderByRankAsc(workbookId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Retourne les postes d'un workbook avec pagination, tries par rang croissant.
     * Utilise pour la page de detail (20 postes par page).
     */
    public Page<WorkplaceDto> findByWorkbookIdPaginated(Long workbookId, Pageable pageable) {
        Page<Workplace> page = workplaceRepository.findByWorkbookIdOrderByRankAsc(workbookId, pageable);
        return page.map(this::toDto);
    }

    /**
     * Retourne un workplace par son identifiant.
     * Leve une exception si le workplace est introuvable.
     */
    public WorkplaceDto findById(Long id) {
        return toDto(findEntityById(id));
    }

    /**
     * Ajoute un nouveau poste en tete de liste (rang 1).
     * Tous les postes existants sont decales d'un rang vers le bas.
     * Si le nouveau poste est marque "actuel", le flag est retire des autres postes.
     */
    public WorkplaceDto addWorkplace(Long workbookId, WorkplaceDto workplaceDto) {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new EntityNotFoundException("Workbook introuvable avec l'id : " + workbookId));

        Workplace workplace = toEntity(workplaceDto);
        workplace.setId(null); // Force la creation d'un nouvel enregistrement

        // Si ce poste est actuel, on retire le flag des autres postes du workbook
        if (workplace.isCurrent()) {
            clearCurrentFlag(workbookId);
        }

        // Decalage de tous les rangs existants de +1 pour liberer la position 1
        workplaceRepository.incrementRanksFrom(workbookId, 1);

        // Flush et clear necessaires pour eviter les conflits de rang en base
        entityManager.flush();
        entityManager.clear();

        workplace.setWorkbook(workbook);
        workplace.setRank(1); // Le nouveau poste prend la premiere position
        return toDto(workplaceRepository.save(workplace));
    }

    /**
     * Met a jour les informations d'un poste existant.
     * Si le poste devient "actuel", le flag est retire des autres postes du meme workbook.
     */
    public WorkplaceDto updateWorkplace(Long workplaceId, WorkplaceDto updated) {
        Workplace existing = findEntityById(workplaceId);

        // Gestion du flag "actuel" : un seul poste peut etre actuel par workbook
        if (updated.isCurrent() && !existing.isCurrent()) {
            clearCurrentFlag(existing.getWorkbook().getId());
            // Flush et refresh necessaires apres la mise a jour du flag
            entityManager.flush();
            entityManager.clear();
            existing = findEntityById(workplaceId);
        }

        // Mise a jour des champs du poste
        existing.setCurrent(updated.isCurrent());
        existing.setCompanyCode(updated.getCompanyCode());
        existing.setCompanyName(updated.getCompanyName());
        existing.setCountryCode(updated.getCountryCode());
        existing.setCountryName(updated.getCountryName());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());

        return toDto(workplaceRepository.save(existing));
    }

    /**
     * Supprime un poste et reajuste les rangs des postes suivants.
     * Les rangs restent consecutifs apres la suppression (pas de trou).
     */
    public void deleteWorkplace(Long workplaceId) {
        Workplace workplace = findEntityById(workplaceId);
        Long workbookId = workplace.getWorkbook().getId();
        int deletedRank = workplace.getRank();

        workplaceRepository.delete(workplace);
        entityManager.flush();

        // Decrement des rangs de tous les postes situes apres le poste supprime
        workplaceRepository.decrementRanksAfter(workbookId, deletedRank);
    }

    /**
     * Remonte un poste d'une position dans la liste.
     * Echange les rangs avec le poste immediatement au-dessus.
     * Sans effet si le poste est deja en premiere position.
     */
    public void moveUp(Long workplaceId) {
        Workplace workplace = findEntityById(workplaceId);

        // Deja en premiere position, rien a faire
        if (workplace.getRank() <= 1) return;

        Long workbookId = workplace.getWorkbook().getId();
        int currentRank = workplace.getRank();
        int targetRank = currentRank - 1;

        // Passage temporaire du voisin a -1 pour eviter un conflit de contrainte d'unicite
        workplaceRepository.findByWorkbookIdAndRank(workbookId, targetRank)
                .ifPresent(other -> {
                    other.setRank(-1);
                    workplaceRepository.saveAndFlush(other);
                });

        workplace.setRank(targetRank);
        workplaceRepository.saveAndFlush(workplace);

        // Assignation du rang libere au voisin precedemment deplace a -1
        workplaceRepository.findByWorkbookIdAndRank(workbookId, -1)
                .ifPresent(other -> {
                    other.setRank(currentRank);
                    workplaceRepository.save(other);
                });
    }

    /**
     * Descend un poste d'une position dans la liste.
     * Echange les rangs avec le poste immediatement en-dessous.
     * Sans effet si le poste est deja en derniere position.
     */
    public void moveDown(Long workplaceId) {
        Workplace workplace = findEntityById(workplaceId);
        Long workbookId = workplace.getWorkbook().getId();
        int currentRank = workplace.getRank();

        int maxRank = workplaceRepository.findMaxRankByWorkbookId(workbookId).orElse(1);

        // Deja en derniere position, rien a faire
        if (currentRank >= maxRank) return;

        int targetRank = currentRank + 1;

        // Meme logique que moveUp : passage temporaire a -1 pour eviter le conflit
        workplaceRepository.findByWorkbookIdAndRank(workbookId, targetRank)
                .ifPresent(other -> {
                    other.setRank(-1);
                    workplaceRepository.saveAndFlush(other);
                });

        workplace.setRank(targetRank);
        workplaceRepository.saveAndFlush(workplace);

        workplaceRepository.findByWorkbookIdAndRank(workbookId, -1)
                .ifPresent(other -> {
                    other.setRank(currentRank);
                    workplaceRepository.save(other);
                });
    }

    /**
     * Recherche un workplace en base par son identifiant.
     * Leve EntityNotFoundException s'il est absent.
     */
    private Workplace findEntityById(Long id) {
        return workplaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workplace introuvable avec l'id : " + id));
    }

    /**
     * Convertit un WorkplaceDto en entite Workplace.
     */
    private Workplace toEntity(WorkplaceDto dto) {
        Workplace workplace = new Workplace();
        workplace.setId(dto.getId());
        workplace.setCurrent(dto.isCurrent());
        workplace.setCompanyCode(dto.getCompanyCode());
        workplace.setCompanyName(dto.getCompanyName());
        workplace.setCountryCode(dto.getCountryCode());
        workplace.setCountryName(dto.getCountryName());
        workplace.setStartDate(dto.getStartDate());
        workplace.setEndDate(dto.getEndDate());
        workplace.setRank(dto.getRank());
        return workplace;
    }

    /**
     * Convertit une entite Workplace en WorkplaceDto.
     * L'identifiant du workbook parent est inclus dans le DTO.
     */
    private WorkplaceDto toDto(Workplace workplace) {
        WorkplaceDto dto = new WorkplaceDto();
        dto.setId(workplace.getId());
        dto.setCurrent(workplace.isCurrent());
        dto.setCompanyCode(workplace.getCompanyCode());
        dto.setCompanyName(workplace.getCompanyName());
        dto.setCountryCode(workplace.getCountryCode());
        dto.setCountryName(workplace.getCountryName());
        dto.setStartDate(workplace.getStartDate());
        dto.setEndDate(workplace.getEndDate());
        dto.setRank(workplace.getRank());
        dto.setWorkbookId(workplace.getWorkbook() != null ? workplace.getWorkbook().getId() : null);
        return dto;
    }

    /**
     * Retire le flag "poste actuel" de tous les postes d'un workbook.
     * Appele avant de marquer un nouveau poste comme actuel.
     */
    private void clearCurrentFlag(Long workbookId) {
        workplaceRepository.clearCurrentFlagByWorkbookId(workbookId);
    }
}