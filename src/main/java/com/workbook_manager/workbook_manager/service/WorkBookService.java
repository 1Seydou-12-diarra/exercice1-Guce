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

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkBookService {

    private final WorkBookRepository workbookRepository;

    // Retourne la liste de tous les workbooks, triés par nom

    public List<WorkbookDto> findAll() {
        return workbookRepository.findAllOrderByName()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // Retourne un workbook par son identifiant

    public WorkbookDto findById(Long id) {
        return toDto(findEntityById(id));
    }

    // Crée un nouveau workbook après vérification des doublons
    public WorkbookDto save(WorkbookDto workbookDto) {
        Workbook workbook = toEntity(workbookDto);
        validateUniqueness(workbook);
        return toDto(workbookRepository.save(workbook));
    }

    // Met à jour les informations d'un workbook existant
    public WorkbookDto update(Long id, WorkbookDto updated) {
        Workbook existing = findEntityById(id);

        validateUniquenessForUpdate(updated, id);

        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setBirthdate(updated.getBirthdate());
        existing.setPassportNumber(updated.getPassportNumber());
        existing.setEmail(updated.getEmail());

        return toDto(workbookRepository.save(existing));
    }

    // Supprime un workbook par son identifiant
    public void deleteById(Long id) {
        if (!workbookRepository.existsById(id)) {
            throw new EntityNotFoundException("Workbook introuvable ");
        }
        workbookRepository.deleteById(id);
    }

    // Recherche un workbook en base, lève une exception s'il est absent
    private Workbook findEntityById(Long id) {
        return workbookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workbook introuvable avec l'id : " + id));
    }

    // Convertit un DTO en entité Workbook
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

    // Convertit une entité Workbook en DTO, avec ses postes triés par rang
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
                .sorted(Comparator.comparing(Workplace::getRank, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toWorkplaceDto)
                .toList());
        return dto;
    }

    // Convertit une entité Workplace en DTO
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

    // Vérifie qu'aucun autre workbook n'utilise déjà le même passeport ou email
    private void validateUniqueness(Workbook workbook) {
        if (workbookRepository.existsByPassportNumber(workbook.getPassportNumber())) {
            throw new IllegalArgumentException("Ce numéro de passeport est déjà utilisé.");
        }
        if (workbookRepository.existsByEmail(workbook.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
    }

    // Même vérification que validateUniqueness, mais en excluant le workbook en cours de modification
    private void validateUniquenessForUpdate(WorkbookDto workbook, Long id) {
        if (workbookRepository.existsByPassportNumberAndIdNot(workbook.getPassportNumber(), id)) {
            throw new IllegalArgumentException("Ce numéro de passeport est déjà utilisé.");
        }
        if (workbookRepository.existsByEmailAndIdNot(workbook.getEmail(), id)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
    }
}