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

@Controller
@RequestMapping("/workbooks")
@RequiredArgsConstructor
public class WorkBookController {

    private final WorkBookService workbookService;
    private final WorkPlaceService workplaceService;

    // LISTE
    @GetMapping
    public String list(Model model) {
        model.addAttribute("workbooks", workbookService.findAll());
        return "workbook/list";
    }

    // FORM CREATE
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("workbook", new WorkbookDto());
        model.addAttribute("pageTitle", "New Workbook");
        return "workbook/form";
    }

    // CREATE
    @PostMapping
    public String create(@Valid @ModelAttribute("workbook") WorkbookDto workbookDto,
                         BindingResult result,
                         RedirectAttributes redirectAttrs,
                         Model model) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "New Workbook");
            return "workbook/form";
        }

        try {
            WorkbookDto saved = workbookService.save(workbookDto);

            redirectAttrs.addFlashAttribute("success", "Workbook créé avec succès.");
            return "redirect:/workbooks/" + saved.getId();

        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "New Workbook");
            return "workbook/form";
        }
    }

    // DETAIL
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {

        WorkbookDto workbook = workbookService.findById(id);

        model.addAttribute("workbook", workbook);
        model.addAttribute("newWorkplace", new WorkplaceDto());
        model.addAttribute("workplaces", workplaceService.findByWorkbookId(id));

        return "workbook/detail";
    }

    // FORM EDIT
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {

        model.addAttribute("workbook", workbookService.findById(id));
        model.addAttribute("pageTitle", "Update Workbook");

        return "workbook/form";
    }

    // UPDATE
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("workbook") WorkbookDto workbookDto,
                         BindingResult result,
                         RedirectAttributes redirectAttrs,
                         Model model) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Update Workbook");
            return "workbook/form";
        }

        try {
            workbookService.update(id, workbookDto);

            redirectAttrs.addFlashAttribute("success", "Workbook modifié avec succès.");
            return "redirect:/workbooks/" + id;

        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Update Workbook");
            return "workbook/form";
        }
    }

    // DELETE
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttrs) {

        workbookService.deleteById(id);

        redirectAttrs.addFlashAttribute("success", "Workbook supprimé.");
        return "redirect:/workbooks";
    }

    // ADD WORKPLACE
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
            redirectAttrs.addFlashAttribute("success", "Poste ajouté avec succès.");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/workbooks/" + id;
    }

    // EDIT WORKPLACE FORM
    @GetMapping("/{id}/workplaces/{workplaceId}/edit")
    public String editWorkplaceForm(@PathVariable Long id,
                                    @PathVariable Long workplaceId,
                                    Model model) {

        model.addAttribute("workbook", workbookService.findById(id));
        model.addAttribute("workplace", workplaceService.findById(workplaceId));

        return "workbook/workplace-edit";
    }

    // UPDATE WORKPLACE
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
            redirectAttrs.addFlashAttribute("success", "Poste modifié avec succès.");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/workbooks/" + id;
    }

    // DELETE WORKPLACE
    @PostMapping("/{id}/workplaces/{workplaceId}/delete")
    public String deleteWorkplace(@PathVariable Long id,
                                  @PathVariable Long workplaceId,
                                  RedirectAttributes redirectAttrs) {

        workplaceService.deleteWorkplace(workplaceId);
        redirectAttrs.addFlashAttribute("success", "Poste supprimé.");

        return "redirect:/workbooks/" + id;
    }

    // MOVE UP
    @PostMapping("/{id}/workplaces/{workplaceId}/move-up")
    public String moveUp(@PathVariable Long id,
                         @PathVariable Long workplaceId) {

        workplaceService.moveUp(workplaceId);
        return "redirect:/workbooks/" + id;
    }

    // MOVE DOWN
    @PostMapping("/{id}/workplaces/{workplaceId}/move-down")
    public String moveDown(@PathVariable Long id,
                           @PathVariable Long workplaceId) {

        workplaceService.moveDown(workplaceId);
        return "redirect:/workbooks/" + id;
    }
}