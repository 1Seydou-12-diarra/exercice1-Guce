package com.workbook_manager.workbook_manager.controller;

import com.workbook_manager.workbook_manager.dto.WorkbookDto;
import com.workbook_manager.workbook_manager.dto.WorkplaceDto;
import com.workbook_manager.workbook_manager.service.WorkBookService;
import com.workbook_manager.workbook_manager.service.WorkPlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Controleur principal gerant les operations CRUD sur les Workbooks
 * et la gestion des Workplaces associes.
 * Toutes les routes sont prefixees par /workbooks.
 */
@Controller
@RequestMapping("/workbooks")
@RequiredArgsConstructor
public class WorkBookController {

    private final WorkBookService workbookService;
    private final WorkPlaceService workplaceService;

    /**
     * Affiche la liste de tous les workbooks.
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("workbooks", workbookService.findAll());
        return "workbook/list";
    }

    /**
     * Affiche le formulaire de creation d'un nouveau workbook.
     * La liste des workplaces est vide au depart.
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("workbook", new WorkbookDto());
        model.addAttribute("workplacesJson", "[]");
        model.addAttribute("pageTitle", "Nouveau Workbook");
        return "workbook/form";
    }

    /**
     * Traite la soumission du formulaire de creation.
     * Le workbook et ses workplaces sont persistes en une seule transaction.
     * En cas d'erreur de validation, le formulaire est reaffiche avec les donnees saisies.
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("workbook") WorkbookDto workbookDto,
                         BindingResult result,
                         RedirectAttributes redirectAttrs,
                         Model model) {

        // Retour au formulaire si des erreurs de validation sont detectees
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Nouveau Workbook");
            model.addAttribute("workplacesJson", toJson(workbookDto.getWorkplaces()));
            return "workbook/form";
        }

        try {
            WorkbookDto saved = workbookService.save(workbookDto);
            redirectAttrs.addFlashAttribute("success", "Workbook cree avec succes.");
            return "redirect:/workbooks/" + saved.getId();

        } catch (Exception e) {
            // Erreur metier (passeport ou email deja utilise)
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Nouveau Workbook");
            model.addAttribute("workplacesJson", toJson(workbookDto.getWorkplaces()));
            return "workbook/form";
        }
    }

    /**
     * Affiche la page de detail d'un workbook avec ses workplaces pagines.
     * 20 workplaces sont affiches par page, tries par rang croissant.
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        WorkbookDto workbook = workbookService.findById(id);

        // Recuperation des workplaces avec pagination (3 par page)
        Page<WorkplaceDto> workplacePage = workplaceService.findByWorkbookIdPaginated(
                id, PageRequest.of(page, 3));

        model.addAttribute("workbook", workbook);
        model.addAttribute("newWorkplace", new WorkplaceDto());
        model.addAttribute("workplacePage", workplacePage);
        model.addAttribute("workplaces", workplacePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", workplacePage.getTotalPages());
        model.addAttribute("totalElements", workplacePage.getTotalElements());
        return "workbook/detail";
    }

    /**
     * Affiche le formulaire de modification d'un workbook existant.
     * Les workplaces existants sont charges depuis la base et injectes en JSON
     * pour etre affiches dans le tableau de l'onglet 2.
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        WorkbookDto workbook = workbookService.findById(id);
        model.addAttribute("workbook", workbook);
        // Serialisation des workplaces en JSON pour le composant JS de l'onglet 2
        model.addAttribute("workplacesJson", toJson(workbook.getWorkplaces()));
        model.addAttribute("pageTitle", "Modifier le Workbook");
        return "workbook/form";
    }

    /**
     * Traite la soumission du formulaire de modification.
     * Les workplaces de l'onglet 2 remplacent completement les anciens en base.
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("workbook") WorkbookDto workbookDto,
                         BindingResult result,
                         RedirectAttributes redirectAttrs,
                         Model model) {

        // Retour au formulaire si des erreurs de validation sont detectees
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Modifier le Workbook");
            model.addAttribute("workplacesJson", toJson(workbookDto.getWorkplaces()));
            return "workbook/form";
        }

        try {
            workbookService.update(id, workbookDto);
            redirectAttrs.addFlashAttribute("success", "Workbook modifie avec succes.");
            return "redirect:/workbooks/" + id;

        } catch (Exception e) {
            // Erreur metier (passeport ou email deja utilise par un autre workbook)
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Modifier le Workbook");
            model.addAttribute("workplacesJson", toJson(workbookDto.getWorkplaces()));
            return "workbook/form";
        }
    }

    /**
     * Supprime un workbook et tous ses workplaces associes.
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        workbookService.deleteById(id);
        redirectAttrs.addFlashAttribute("success", "Workbook supprime.");
        return "redirect:/workbooks";
    }

    /**
     * Ajoute un nouveau workplace a un workbook existant depuis la page detail.
     * Le rang est assigne automatiquement (insertion en tete de liste).
     */
    @PostMapping("/{id}/workplaces")
    public String addWorkplace(@PathVariable Long id,
                               @Valid @ModelAttribute("newWorkplace") WorkplaceDto workplaceDto,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttrs) {

        if (result.hasErrors()) {
            model.addAttribute("workbook", workbookService.findById(id));
            model.addAttribute("workplaces", workplaceService.findByWorkbookId(id));
            return "workbook/detail";
        }

        try {
            workplaceService.addWorkplace(id, workplaceDto);
            redirectAttrs.addFlashAttribute("success", "Poste ajoute avec succes.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/workbooks/" + id;
    }

    /**
     * Affiche le formulaire de modification d'un workplace existant.
     */
    @GetMapping("/{id}/workplaces/{workplaceId}/edit")
    public String editWorkplaceForm(@PathVariable Long id,
                                    @PathVariable Long workplaceId,
                                    Model model) {
        model.addAttribute("workbook", workbookService.findById(id));
        model.addAttribute("workplace", workplaceService.findById(workplaceId));
        return "workbook/workplace-edit";
    }

    /**
     * Traite la modification d'un workplace existant.
     * Si le workplace devient "actuel", le flag est retire des autres postes du workbook.
     */
    @PostMapping("/{id}/workplaces/{workplaceId}/edit")
    public String updateWorkplace(@PathVariable Long id,
                                  @PathVariable Long workplaceId,
                                  @Valid @ModelAttribute("workplace") WorkplaceDto workplaceDto,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttrs) {

        if (result.hasErrors()) {
            model.addAttribute("workbook", workbookService.findById(id));
            model.addAttribute("workplaces", workplaceService.findByWorkbookId(id));
            return "workbook/workplace-edit";
        }

        try {
            workplaceService.updateWorkplace(workplaceId, workplaceDto);
            redirectAttrs.addFlashAttribute("success", "Poste modifie avec succes.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/workbooks/" + id;
    }

    /**
     * Supprime un workplace et reordonne les rangs des postes suivants.
     */
    @PostMapping("/{id}/workplaces/{workplaceId}/delete")
    public String deleteWorkplace(@PathVariable Long id,
                                  @PathVariable Long workplaceId,
                                  RedirectAttributes redirectAttrs) {
        workplaceService.deleteWorkplace(workplaceId);
        redirectAttrs.addFlashAttribute("success", "Poste supprime.");
        return "redirect:/workbooks/" + id;
    }

    /**
     * Remonte un workplace d'une position dans la liste (echange avec le poste au-dessus).
     */
    @PostMapping("/{id}/workplaces/{workplaceId}/move-up")
    public String moveUp(@PathVariable Long id, @PathVariable Long workplaceId) {
        workplaceService.moveUp(workplaceId);
        return "redirect:/workbooks/" + id;
    }

    /**
     * Descend un workplace d'une position dans la liste (echange avec le poste en-dessous).
     */
    @PostMapping("/{id}/workplaces/{workplaceId}/move-down")
    public String moveDown(@PathVariable Long id, @PathVariable Long workplaceId) {
        workplaceService.moveDown(workplaceId);
        return "redirect:/workbooks/" + id;
    }

    /**
     * Serialise une liste de WorkplaceDto en JSON manuellement.
     * Utilise pour injecter les workplaces dans l'attribut data-json du template.
     * On n'utilise pas Jackson directement pour eviter les problemes de resolution de methode.
     */
    private String toJson(List<WorkplaceDto> workplaces) {
        if (workplaces == null || workplaces.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < workplaces.size(); i++) {
            WorkplaceDto w = workplaces.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
                    .append("\"companyCode\":").append(jsonStr(w.getCompanyCode())).append(",")
                    .append("\"companyName\":").append(jsonStr(w.getCompanyName())).append(",")
                    .append("\"countryCode\":").append(jsonStr(w.getCountryCode())).append(",")
                    .append("\"countryName\":").append(jsonStr(w.getCountryName())).append(",")
                    .append("\"startDate\":").append(jsonStr(w.getStartDate() != null ? w.getStartDate().toString() : null)).append(",")
                    .append("\"endDate\":").append(jsonStr(w.getEndDate() != null ? w.getEndDate().toString() : null)).append(",")
                    .append("\"current\":").append(w.isCurrent()).append(",")
                    .append("\"rank\":").append(w.getRank() != null ? w.getRank() : 0)
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Echappe une valeur String pour l'inclure dans un objet JSON.
     * Retourne null si la valeur est nulle, sinon une chaine entre guillemets.
     */
    private String jsonStr(String val) {
        if (val == null) return "null";
        return "\"" + val.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}