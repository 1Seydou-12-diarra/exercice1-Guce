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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkPlaceService {

    private final WorkPlaceRepository workplaceRepository;
    private final WorkBookRepository workbookRepository;
    private final EntityManager entityManager;

    // Retourne tous les postes d'un workbook, triés par rang croissant

    public List<WorkplaceDto> findByWorkbookId(Long workbookId) {
        return workplaceRepository.findByWorkbookIdOrderByRankAsc(workbookId)
                .stream()
                .map(this::toDto)
                .toList();
    }


    // Retourne les postes d'un workbook avec pagination
    public Page<WorkplaceDto> findByWorkbookIdPaginated(Long workbookId, Pageable pageable) {
        Page<Workplace> page = workplaceRepository.findByWorkbookIdOrderByRankAsc(workbookId, pageable);
        return page.map(this::toDto);
    }

    // Retourne un poste par son identifiant

    public WorkplaceDto findById(Long id) {
        return toDto(findEntityById(id));
    }

    // Ajoute un nouveau poste en tête de liste (rang 1) et décale les autres vers le bas
    public WorkplaceDto addWorkplace(Long workbookId, WorkplaceDto workplaceDto) {
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new EntityNotFoundException("Workbook introuvable avec l'id : " + workbookId));

        Workplace workplace = toEntity(workplaceDto);
        workplace.setId(null);

        // Si ce poste est marqué comme actuel, on retire le flag des autres postes
        if (workplace.isCurrent()) {
            clearCurrentFlag(workbookId);
        }

        // Décale tous les rangs existants de +1 pour libérer la place au rang 1
        workplaceRepository.incrementRanksFrom(workbookId, 1);

        entityManager.flush();
        entityManager.clear();

        workplace.setWorkbook(workbook);
        workplace.setRank(1);
        return toDto(workplaceRepository.save(workplace));
    }

    // Met à jour les informations d'un poste existant
    public WorkplaceDto updateWorkplace(Long workplaceId, WorkplaceDto updated) {
        Workplace existing = findEntityById(workplaceId);

        // Si le poste devient "actuel", on retire le flag des autres postes d'abord
        if (updated.isCurrent() && !existing.isCurrent()) {
            clearCurrentFlag(existing.getWorkbook().getId());
            entityManager.flush();
            entityManager.clear();
            existing = findEntityById(workplaceId);
        }

        existing.setCurrent(updated.isCurrent());
        existing.setCompanyCode(updated.getCompanyCode());
        existing.setCompanyName(updated.getCompanyName());
        existing.setCountryCode(updated.getCountryCode());
        existing.setCountryName(updated.getCountryName());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());

        return toDto(workplaceRepository.save(existing));
    }

    // Supprime un poste et réajuste les rangs des postes suivants
    public void deleteWorkplace(Long workplaceId) {
        Workplace workplace = findEntityById(workplaceId);
        Long workbookId = workplace.getWorkbook().getId();
        int deletedRank = workplace.getRank();

        workplaceRepository.delete(workplace);
        entityManager.flush();

        workplaceRepository.decrementRanksAfter(workbookId, deletedRank);
    }

    // Remonte un poste d'une position dans la liste (échange avec le poste au-dessus)
    public void moveUp(Long workplaceId) {
        Workplace workplace = findEntityById(workplaceId);

        // Déjà en première position, rien à faire
        if (workplace.getRank() <= 1) return;

        Long workbookId = workplace.getWorkbook().getId();
        int currentRank = workplace.getRank();
        int targetRank = currentRank - 1;

        // On passe le voisin du dessus à -1 temporairement pour éviter un conflit d'unicité
        workplaceRepository.findByWorkbookIdAndRank(workbookId, targetRank)
                .ifPresent(other -> {
                    other.setRank(-1);
                    workplaceRepository.saveAndFlush(other);
                });

        workplace.setRank(targetRank);
        workplaceRepository.saveAndFlush(workplace);

        // On assigne l'ancien rang au voisin déplacé temporairement
        workplaceRepository.findByWorkbookIdAndRank(workbookId, -1)
                .ifPresent(other -> {
                    other.setRank(currentRank);
                    workplaceRepository.save(other);
                });
    }

    // Descend un poste d'une position dans la liste (échange avec le poste en-dessous)
    public void moveDown(Long workplaceId) {
        Workplace workplace = findEntityById(workplaceId);
        Long workbookId = workplace.getWorkbook().getId();
        int currentRank = workplace.getRank();

        int maxRank = workplaceRepository.findMaxRankByWorkbookId(workbookId).orElse(1);

        // Déjà en dernière position, rien à faire
        if (currentRank >= maxRank) return;

        int targetRank = currentRank + 1;

        // Même logique que moveUp : passage temporaire à -1 pour éviter le conflit
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

    // Recherche un poste en base, lève une exception s'il est absent
    private Workplace findEntityById(Long id) {
        return workplaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workplace introuvable avec l'id : " + id));
    }

    // Convertit un DTO en entité Workplace
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

    // Convertit une entité Workplace en DTO
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

    // Retire le flag "poste actuel" de tous les postes d'un workbook
    private void clearCurrentFlag(Long workbookId) {
        workplaceRepository.clearCurrentFlagByWorkbookId(workbookId);
    }
}