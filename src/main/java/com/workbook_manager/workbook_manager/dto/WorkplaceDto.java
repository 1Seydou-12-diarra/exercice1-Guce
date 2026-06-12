package com.workbook_manager.workbook_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkplaceDto {

    private Long id;
    private boolean current;

    @NotBlank(message = "Le code entreprise est obligatoire")
    private String companyCode;

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String companyName;

    @NotBlank(message = "Le code pays est obligatoire")
    private String countryCode;

    @NotBlank(message = "Le nom du pays est obligatoire")
    private String countryName;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    private LocalDate endDate;
    private Integer rank;
    private Long workbookId;
}
