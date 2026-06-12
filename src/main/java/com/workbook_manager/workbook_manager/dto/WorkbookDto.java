package com.workbook_manager.workbook_manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkbookDto {

    private Long id;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate birthdate;

    @NotBlank(message = "Le numéro de passeport est obligatoire")
    private String passportNumber;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    private List<WorkplaceDto> workplaces = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Integer getAge() {
        if (birthdate == null) {
            return null;
        }
        return Period.between(birthdate, LocalDate.now()).getYears();
    }
}
