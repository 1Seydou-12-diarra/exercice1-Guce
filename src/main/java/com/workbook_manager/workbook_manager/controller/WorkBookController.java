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
        // Récupère tous les workbooks triés par nom et les envoie à la vue
        model.addAttribute("workbooks", workbookService.findAll());
        return "workbook/list";
    }

    // Affiche le formulaire de création d'un nouveau workbook
// GET /workbooks/new
    @GetMapping("/new")
    public String createForm(Model model) {
        // Envoie un objet Workbook vide à la vue pour lier le formulaire
        model.addAttribute("workbook", new Workbook());
        model.addAttribute("pageTitle", "Nouveau Workbook");
        return "workbook/form";
    }

    // Traite la soumission du formulaire de création
// POST /workbooks
    @PostMapping
    public String create(@Valid @ModelAttribute("workbook") Workbook workbook,
                         BindingResult result,       // Contient les erreurs de validation
                         RedirectAttributes redirectAttrs,
                         Model model) {

        // Si le formulaire contient des erreurs, on réaffiche le formulaire avec les messages
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Nouveau Workbook");
            return "workbook/form";
        }
        try {
            // Sauvegarde le workbook et redirige vers sa page de détail
            Workbook saved = workbookService.save(workbook);
            redirectAttrs.addFlashAttribute("success", "Workbook créé avec succès.");
            return "redirect:/workbooks/" + saved.getId();
        } catch (IllegalArgumentException e) {
            // En cas de doublon (passeport ou email), on réaffiche le formulaire avec l'erreur
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Nouveau Workbook");
            return "workbook/form";
        }
    }

    // Affiche la page de détail d'un workbook avec ses postes
// GET /workbooks/{id}
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Workbook workbook = workbookService.findById(id);
        // Envoie le workbook à la vue
        model.addAttribute("workbook", workbook);
        // Envoie un Workplace vide pour le formulaire d'ajout de poste
        model.addAttribute("newWorkplace", new Workplace());
        return "workbook/detail";
    }

    // Affiche le formulaire de modification d'un workbook existant
// GET /workbooks/{id}/edit
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        // Charge le workbook existant et le transmet au formulaire
        model.addAttribute("workbook", workbookService.findById(id));
        model.addAttribute("pageTitle", "Modifier le Workbook");
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

        // Si le formulaire contient des erreurs, on réaffiche le formulaire
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Modifier le Workbook");
            return "workbook/form";
        }
        try {
            // Met à jour le workbook et redirige vers sa page de détail
            workbookService.update(id, workbook);
            redirectAttrs.addFlashAttribute("success", "Workbook modifié avec succès.");
            return "redirect:/workbooks/" + id;
        } catch (IllegalArgumentException e) {
            // En cas de doublon (passeport ou email), on réaffiche le formulaire avec l'erreur
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
        // Redirige vers la liste après suppression
        return "redirect:/workbooks";
    }

// ─────────────────────────────────────────────
//  GESTION DES POSTES (depuis la vue détail)
// ─────────────────────────────────────────────

    // Traite l'ajout d'un nouveau poste depuis le formulaire de la page de détail
// POST /workbooks/{id}/workplaces
    @PostMapping("/{id}/workplaces")
    public String addWorkplace(@PathVariable Long id,
                               @Valid @ModelAttribute("newWorkplace") Workplace workplace,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttrs) {

        // Si le formulaire contient des erreurs, on réaffiche la page de détail
        if (result.hasErrors()) {
            model.addAttribute("workbook", workbookService.findById(id));
            return "workbook/detail";
        }
        try {
            // Ajoute le poste au workbook (rang calculé automatiquement)
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
        // Charge le workbook et le poste concerné pour préremplir le formulaire
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

        // Si le formulaire contient des erreurs, on réaffiche le formulaire de modification
        if (result.hasErrors()) {
            model.addAttribute("workbook", workbookService.findById(id));
            return "workbook/workplace-edit";
        }
        try {
            // Met à jour le poste et redirige vers la page de détail du workbook
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
        redirectAttrs.addFlashAttribute("success", "Poste supprimé.");
        // Redirige vers la page de détail du workbook
        return "redirect:/workbooks/" + id;
    }

    // Remonte un poste d'une position dans la liste (échange avec le poste du rang précédent)
   // POST /workbooks/{id}/workplaces/{workplaceId}/move-up
    @PostMapping("/{id}/workplaces/{workplaceId}/move-up")
    public String moveUp(@PathVariable Long id,
                         @PathVariable Long workplaceId,
                         RedirectAttributes redirectAttrs) {
        workplaceService.moveUp(workplaceId);
        // Redirige vers la page de détail pour voir le nouvel ordre
        return "redirect:/workbooks/" + id;
    }

    // Descend un poste d'une position dans la liste (échange avec le poste du rang suivant)
    // POST /workbooks/{id}/workplaces/{workplaceId}/move-down
    @PostMapping("/{id}/workplaces/{workplaceId}/move-down")
    public String moveDown(@PathVariable Long id,
                           @PathVariable Long workplaceId,
                           RedirectAttributes redirectAttrs) {
        workplaceService.moveDown(workplaceId);
        // Redirige vers la page de détail pour voir le nouvel ordre
        return "redirect:/workbooks/" + id;
    }
}
