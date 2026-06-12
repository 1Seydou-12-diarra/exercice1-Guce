package com.workbook_manager.workbook_manager.service;

import com.workbook_manager.workbook_manager.dto.WorkbookDto;
import com.workbook_manager.workbook_manager.dto.WorkplaceDto;
import com.workbook_manager.workbook_manager.entite.Workbook;
import com.workbook_manager.workbook_manager.entite.Workplace;
import com.workbook_manager.workbook_manager.repository.WorkBookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkBookService {

    private final WorkBookRepository workbookRepository;

    // ── Lecture ──────────────────────────────────────────────────────────────

    public List<WorkbookDto> findAll() {
        return workbookRepository.findAllOrderByName()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public WorkbookDto findById(Long id) {
        return toDto(findEntityById(id));
    }

    // ── Création : workbook + workplaces en une transaction ──────────────────

    public WorkbookDto save(WorkbookDto workbookDto) {
        validateUniqueness(workbookDto);

        Workbook workbook = toEntity(workbookDto);

        // Attacher les workplaces transmis depuis le formulaire
        if (workbookDto.getWorkplaces() != null && !workbookDto.getWorkplaces().isEmpty()) {
            List<Workplace> workplaces = buildWorkplaces(workbookDto.getWorkplaces(), workbook);
            workbook.setWorkplaces(workplaces);
        }

        return toDto(workbookRepository.save(workbook));
    }

    // ── Modification : workbook + remplacement complet des workplaces ─────────

    public WorkbookDto update(Long id, WorkbookDto updated) {
        Workbook existing = findEntityById(id);

        validateUniquenessForUpdate(updated, id);

        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setBirthdate(updated.getBirthdate());
        existing.setPassportNumber(updated.getPassportNumber());
        existing.setEmail(updated.getEmail());

        // Remplacement complet : on efface les anciens workplaces et on remet les nouveaux.
        // orphanRemoval = true sur la relation → les anciens seront supprimés automatiquement.
        existing.getWorkplaces().clear();

        if (updated.getWorkplaces() != null && !updated.getWorkplaces().isEmpty()) {
            List<Workplace> workplaces = buildWorkplaces(updated.getWorkplaces(), existing);
            existing.getWorkplaces().addAll(workplaces);
        }

        return toDto(workbookRepository.save(existing));
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    public void deleteById(Long id) {
        if (!workbookRepository.existsById(id)) {
            throw new EntityNotFoundException("Workbook introuvable");
        }
        workbookRepository.deleteById(id);
    }

    // ── Helpers internes ─────────────────────────────────────────────────────

    /**
     * Construit la liste des entités Workplace à partir des DTOs.
     * - Les rangs sont réassignés séquentiellement (1, 2, 3…).
     * - Si plusieurs workplaces sont marqués "current", seul le dernier le reste.
     */
    private List<Workplace> buildWorkplaces(List<WorkplaceDto> dtos, Workbook workbook) {
        // Tri défensif par rang entrant, puis réassignation propre
        List<WorkplaceDto> sorted = dtos.stream()
                .sorted(Comparator.comparingInt(wp ->
                        wp.getRank() != null ? wp.getRank() : Integer.MAX_VALUE))
                .toList();

        List<Workplace> result = new ArrayList<>();

        // Un seul "current" autorisé : le premier trouvé dans l'ordre
        boolean currentAssigned = false;

        for (int i = 0; i < sorted.size(); i++) {
            WorkplaceDto dto = sorted.get(i);
            Workplace wp = new Workplace();
            wp.setCompanyCode(dto.getCompanyCode());
            wp.setCompanyName(dto.getCompanyName());
            wp.setCountryCode(dto.getCountryCode());
            wp.setCountryName(dto.getCountryName());
            wp.setStartDate(dto.getStartDate());
            wp.setEndDate(dto.getEndDate());
            wp.setRank(i + 1);  // rang réassigné proprement
            wp.setWorkbook(workbook);

            boolean isCurrent = dto.isCurrent() && !currentAssigned;
            wp.setCurrent(isCurrent);
            if (isCurrent) currentAssigned = true;

            result.add(wp);
        }

        return result;
    }

    private Workbook findEntityById(Long id) {
        return workbookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workbook introuvable avec l'id : " + id));
    }

    private Workbook toEntity(WorkbookDto dto) {
        Workbook workbook = new Workbook();
        workbook.setId(dto.getId());
        workbook.setFirstName(dto.getFirstName());
        workbook.setLastName(dto.getLastName());
        workbook.setBirthdate(dto.getBirthdate());
        workbook.setPassportNumber(dto.getPassportNumber());
        workbook.setEmail(dto.getEmail());
        return workbook;
    }

    private WorkbookDto toDto(Workbook workbook) {
        WorkbookDto dto = new WorkbookDto();
        dto.setId(workbook.getId());
        dto.setFirstName(workbook.getFirstName());
        dto.setLastName(workbook.getLastName());
        dto.setBirthdate(workbook.getBirthdate());
        dto.setPassportNumber(workbook.getPassportNumber());
        dto.setEmail(workbook.getEmail());
        dto.setWorkplaces(workbook.getWorkplaces()
                .stream()
                .sorted(Comparator.comparing(Workplace::getRank,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(this::toWorkplaceDto)
                .toList());
        return dto;
    }

    private WorkplaceDto toWorkplaceDto(Workplace workplace) {
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

    private void validateUniqueness(WorkbookDto dto) {
        if (workbookRepository.existsByPassportNumber(dto.getPassportNumber())) {
            throw new IllegalArgumentException("Ce numéro de passeport est déjà utilisé.");
        }
        if (workbookRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
    }

    private void validateUniquenessForUpdate(WorkbookDto dto, Long id) {
        if (workbookRepository.existsByPassportNumberAndIdNot(dto.getPassportNumber(), id)) {
            throw new IllegalArgumentException("Ce numéro de passeport est déjà utilisé.");
        }
        if (workbookRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
    }
}