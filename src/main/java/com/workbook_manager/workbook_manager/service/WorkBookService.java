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

/**
 * Service metier gerant les operations sur les Workbooks.
 * Toutes les methodes sont transactionnelles : en cas d'erreur,
 * les modifications sont annulees automatiquement.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkBookService {

    private final WorkBookRepository workbookRepository;

    /**
     * Retourne tous les workbooks tries par nom.
     */
    public List<WorkbookDto> findAll() {
        return workbookRepository.findAllOrderByName()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Retourne un workbook par son identifiant.
     * Leve une exception si le workbook est introuvable.
     */
    public WorkbookDto findById(Long id) {
        return toDto(findEntityById(id));
    }

    /**
     * Cree un nouveau workbook avec ses workplaces en une seule transaction.
     * Verifie l'unicite du passeport et de l'email avant la sauvegarde.
     */
    public WorkbookDto save(WorkbookDto workbookDto) {
        // Verification de l'unicite du passeport et de l'email
        validateUniqueness(workbookDto);

        Workbook workbook = toEntity(workbookDto);

        // Attacher les workplaces transmis depuis le formulaire si presents
        if (workbookDto.getWorkplaces() != null && !workbookDto.getWorkplaces().isEmpty()) {
            List<Workplace> workplaces = buildWorkplaces(workbookDto.getWorkplaces(), workbook);
            workbook.setWorkplaces(workplaces);
        }

        return toDto(workbookRepository.save(workbook));
    }

    /**
     * Modifie un workbook existant et remplace completement sa liste de workplaces.
     * Grace a orphanRemoval = true, les anciens workplaces sont supprimes automatiquement.
     */
    public WorkbookDto update(Long id, WorkbookDto updated) {
        Workbook existing = findEntityById(id);

        // Verification de l'unicite en excluant le workbook en cours de modification
        validateUniquenessForUpdate(updated, id);

        // Mise a jour des champs du workbook
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setBirthdate(updated.getBirthdate());
        existing.setPassportNumber(updated.getPassportNumber());
        existing.setEmail(updated.getEmail());

        // Remplacement complet des workplaces :
        // orphanRemoval = true supprime automatiquement les anciens en base
        existing.getWorkplaces().clear();

        if (updated.getWorkplaces() != null && !updated.getWorkplaces().isEmpty()) {
            List<Workplace> workplaces = buildWorkplaces(updated.getWorkplaces(), existing);
            existing.getWorkplaces().addAll(workplaces);
        }

        return toDto(workbookRepository.save(existing));
    }

    /**
     * Supprime un workbook et tous ses workplaces associes.
     * Leve une exception si le workbook est introuvable.
     */
    public void deleteById(Long id) {
        if (!workbookRepository.existsById(id)) {
            throw new EntityNotFoundException("Workbook introuvable");
        }
        workbookRepository.deleteById(id);
    }

    /**
     * Construit la liste des entites Workplace a partir des DTOs recus du formulaire.
     * - Les rangs sont reassignes sequentiellement (1, 2, 3...) pour eviter les trous.
     * - Un seul workplace peut etre marque comme "actuel" : le premier trouve dans l'ordre.
     * - Chaque workplace est rattache au workbook parent.
     */
    private List<Workplace> buildWorkplaces(List<WorkplaceDto> dtos, Workbook workbook) {
        // Tri defensif par rang entrant pour respecter l'ordre saisi par l'utilisateur
        List<WorkplaceDto> sorted = dtos.stream()
                .sorted(Comparator.comparingInt(wp ->
                        wp.getRank() != null ? wp.getRank() : Integer.MAX_VALUE))
                .toList();

        List<Workplace> result = new ArrayList<>();
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
            wp.setRank(i + 1); // rang reassigne proprement a partir de 1
            wp.setWorkbook(workbook);

            // Seul le premier workplace marque "actuel" conserve ce statut
            boolean isCurrent = dto.isCurrent() && !currentAssigned;
            wp.setCurrent(isCurrent);
            if (isCurrent) currentAssigned = true;

            result.add(wp);
        }

        return result;
    }

    /**
     * Recherche un workbook en base par son identifiant.
     * Leve EntityNotFoundException s'il est absent.
     */
    private Workbook findEntityById(Long id) {
        return workbookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workbook introuvable avec l'id : " + id));
    }

    /**
     * Convertit un WorkbookDto en entite Workbook (sans les workplaces).
     */
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

    /**
     * Convertit une entite Workbook en WorkbookDto.
     * Les workplaces sont inclus, tries par rang croissant.
     */
    private WorkbookDto toDto(Workbook workbook) {
        WorkbookDto dto = new WorkbookDto();
        dto.setId(workbook.getId());
        dto.setFirstName(workbook.getFirstName());
        dto.setLastName(workbook.getLastName());
        dto.setBirthdate(workbook.getBirthdate());
        dto.setPassportNumber(workbook.getPassportNumber());
        dto.setEmail(workbook.getEmail());
        // Inclusion des workplaces tries par rang croissant (nulls en dernier)
        dto.setWorkplaces(workbook.getWorkplaces()
                .stream()
                .sorted(Comparator.comparing(Workplace::getRank,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(this::toWorkplaceDto)
                .toList());
        return dto;
    }

    /**
     * Convertit une entite Workplace en WorkplaceDto.
     */
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

    /**
     * Verifie que le passeport et l'email ne sont pas deja utilises lors d'une creation.
     */
    private void validateUniqueness(WorkbookDto dto) {
        if (workbookRepository.existsByPassportNumber(dto.getPassportNumber())) {
            throw new IllegalArgumentException("Ce numero de passeport est deja utilise.");
        }
        if (workbookRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est deja utilise.");
        }
    }

    /**
     * Verifie que le passeport et l'email ne sont pas deja utilises par un autre workbook
     * lors d'une modification (exclut le workbook en cours de modification).
     */
    private void validateUniquenessForUpdate(WorkbookDto dto, Long id) {
        if (workbookRepository.existsByPassportNumberAndIdNot(dto.getPassportNumber(), id)) {
            throw new IllegalArgumentException("Ce numero de passeport est deja utilise.");
        }
        if (workbookRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new IllegalArgumentException("Cet email est deja utilise.");
        }
    }
}