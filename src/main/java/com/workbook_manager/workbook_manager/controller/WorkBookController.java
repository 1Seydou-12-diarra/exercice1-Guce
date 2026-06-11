package com.workbook_manager.workbook_manager.controller;

import com.workbook_manager.workbook_manager.entite.Workbook;
import com.workbook_manager.workbook_manager.entite.Workplace;
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

    // Injection des services nécessaires au contrôleur
    private final WorkBookService workbookService;
    private final WorkPlaceService workplaceService;

// ─────────────────────────────────────────────
//  CRUD WORKBOOK
// ─────────────────────────────────────────────

    // Affiche la liste de tous les workbooks
    // GET /workbooks
    @GetMapping
    public String list(Model model) {
        model.addAttribute("workbooks", workbookService.findAll());
        return "workbook/list";
    }

    // Affiche le formulaire de création d'un nouveau workbook
    // GET /workbooks/new
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("workbook", new Workbook());
        model.addAttribute("pageTitle", "New Workbook");
        return "workbook/form";
    }

    // Traite la soumission du formulaire de création
    // POST /workbooks
    @PostMapping
    public String create(@Valid @ModelAttribute("workbook") Workbook workbook,
                         BindingResult result,
                         RedirectAttributes redirectAttrs,
                         Model model) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "New Workbook");
            return "workbook/form";
        }
        try {
            Workbook saved = workbookService.save(workbook);
            redirectAttrs.addFlashAttribute("success", "Workbook create .");
            return "redirect:/workbooks/" + saved.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "New Workbook");
            return "workbook/form";
        }
    }

    // Affiche la page de détail d'un workbook avec ses postes
    // GET /workbooks/{id}
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        // Charge le workbook depuis la base
        Workbook workbook = workbookService.findById(id);
        model.addAttribute("workbook", workbook);
        model.addAttribute("newWorkplace", new Workplace());
        // ✅ Charge explicitement les workplaces triés par rang dans la même transaction
        // Évite le problème de chargement LAZY hors session lors du rendu Thymeleaf
        model.addAttribute("workplaces", workplaceService.findByWorkbookId(id));
        return "workbook/detail";
    }

    // Affiche le formulaire de modification d'un workbook existant
    // GET /workbooks/{id}/edit
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("workbook", workbookService.findById(id));
        model.addAttribute("pageTitle", "Update the Workbook");
        return "workbook/form";
    }

    // Traite la soumission du formulaire de modification
    // POST /workbooks/{id}/edit
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("workbook") Workbook workbook,
                         BindingResult result,
                         RedirectAttributes redirectAttrs,
                         Model model) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "update the  Workbook");
            return "workbook/form";
        }
        try {
            workbookService.update(id, workbook);
            redirectAttrs.addFlashAttribute("success", "Workbook modifié avec succès.");
            return "redirect:/workbooks/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Modifier le Workbook");
            return "workbook/form";
        }
    }

    // Supprime un workbook et tous ses postes associés
    // POST /workbooks/{id}/delete
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        workbookService.deleteById(id);
        redirectAttrs.addFlashAttribute("success", "Workbook supprimé.");
        return "redirect:/workbooks";
    }

// ─────────────────────────────────────────────
//  GESTION DES workalaces (depuis la vue détail)
// ─────────────────────────────────────────────

    // Traite l'ajout d'un nouveau poste depuis le formulaire de la page de détail
    // POST /workbooks/{id}/workplaces
    @PostMapping("/{id}/workplaces")
    public String addWorkplace(@PathVariable Long id,
                               @Valid @ModelAttribute("newWorkplace") Workplace workplace,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttrs) {

        // ✅ En cas d'erreur, recharge les workplaces explicitement pour éviter le problème LAZY
        if (result.hasErrors()) {
            model.addAttribute("workbook", workbookService.findById(id));
            model.addAttribute("workplaces", workplaceService.findByWorkbookId(id));
            return "workbook/detail";
        }
        try {
            workplaceService.addWorkplace(id, workplace);
            redirectAttrs.addFlashAttribute("success", "Poste ajouté avec succès.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/workbooks/" + id;
    }

    // Affiche le formulaire de modification d'un poste existant
    // GET /workbooks/{id}/workplaces/{workplaceId}/edit
    @GetMapping("/{id}/workplaces/{workplaceId}/edit")
    public String editWorkplaceForm(@PathVariable Long id,
                                    @PathVariable Long workplaceId,
                                    Model model) {
        model.addAttribute("workbook", workbookService.findById(id));
        model.addAttribute("workplace", workplaceService.findById(workplaceId));
        return "workbook/workplace-edit";
    }

    // Traite la soumission du formulaire de modification d'un poste
    // POST /workbooks/{id}/workplaces/{workplaceId}/edit
    @PostMapping("/{id}/workplaces/{workplaceId}/edit")
    public String updateWorkplace(@PathVariable Long id,
                                  @PathVariable Long workplaceId,
                                  @Valid @ModelAttribute("workplace") Workplace workplace,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttrs) {

        // ✅ En cas d'erreur, recharge les workplaces explicitement pour éviter le problème LAZY
        if (result.hasErrors()) {
            model.addAttribute("workbook", workbookService.findById(id));
            model.addAttribute("workplaces", workplaceService.findByWorkbookId(id));
            return "workbook/workplace-edit";
        }
        try {
            workplaceService.updateWorkplace(workplaceId, workplace);
            redirectAttrs.addFlashAttribute("success", "Poste modifié avec succès.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/workbooks/" + id;
    }

    // Supprime un poste et réajuste les rangs des postes restants
    // POST /workbooks/{id}/workplaces/{workplaceId}/delete
    @PostMapping("/{id}/workplaces/{workplaceId}/delete")
    public String deleteWorkplace(@PathVariable Long id,
                                  @PathVariable Long workplaceId,
                                  RedirectAttributes redirectAttrs) {
        workplaceService.deleteWorkplace(workplaceId);
        redirectAttrs.addFlashAttribute("success", "workplace delete.");
        return "redirect:/workbooks/" + id;
    }

    // Remonte un poste d'une position dans la liste
    // POST /workbooks/{id}/workplaces/{workplaceId}/move-up
    @PostMapping("/{id}/workplaces/{workplaceId}/move-up")
    public String moveUp(@PathVariable Long id,
                         @PathVariable Long workplaceId,
                         RedirectAttributes redirectAttrs) {
        workplaceService.moveUp(workplaceId);
        return "redirect:/workbooks/" + id;
    }

    // Descend un poste d'une position dans la liste
    // POST /workbooks/{id}/workplaces/{workplaceId}/move-down
    @PostMapping("/{id}/workplaces/{workplaceId}/move-down")
    public String moveDown(@PathVariable Long id,
                           @PathVariable Long workplaceId,
                           RedirectAttributes redirectAttrs) {
        workplaceService.moveDown(workplaceId);
        return "redirect:/workbooks/" + id;
    }
}