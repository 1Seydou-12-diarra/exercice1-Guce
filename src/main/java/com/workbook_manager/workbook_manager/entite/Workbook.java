package com.workbook_manager.workbook_manager.entite;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "workbook")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Workbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le prénom est obligatoire")
    @Column(nullable = false)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String lastName;


    @NotNull(message = "La date de naissance est obligatoire")
    @Column(nullable = false)
    private LocalDate birthdate;

    @NotBlank(message = "Le numéro de passeport est obligatoire")
    @Column(nullable = false, unique = true)
    private String passportNumber;


    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Column(nullable = false, unique = true)
    private String email;

    // orphanRemoval : un Workplace retiré de la liste est automatiquement supprimé en base
    // fetch LAZY : les Workplace ne sont chargés qu'au moment où on y accède
    @OneToMany(mappedBy = "workbook", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("rank ASC")
    private List<Workplace> workplaces = new ArrayList<>();

    /**
     * Calcule l'âge à partir de la date de naissance.
     * @Transient : cette valeur n'est pas stockée en base, elle est calculée à la volée.
     */
    @Transient
    public Integer getAge() {

        if (birthdate == null) return null;
        // Calcule le nombre d'années complètes entre la naissance et aujourd'hui
        return Period.between(birthdate, LocalDate.now()).getYears();
    }

    // Retourne le nom complet (prénom + nom) — utilisé dans les templates Thymeleaf
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Retourne les postes triés par rang croissant.
     * Utilisé dans les vues pour garantir un affichage ordonné.
     */
    public List<Workplace> getSortedWorkplaces() {
        return workplaces.stream()
                // Tri par la valeur entière du rang (1, 2, 3...)
                .sorted(Comparator.comparingInt(Workplace::getRank))
                // Convertit le stream en liste non modifiable
                .toList();

}
}
