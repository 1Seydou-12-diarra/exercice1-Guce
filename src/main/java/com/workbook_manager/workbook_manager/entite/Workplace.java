package com.workbook_manager.workbook_manager.entite;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "workplace",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workbook_id", "rank"})
)
@Getter
@Setter
@NoArgsConstructor
public class Workplace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean current = false;

    @NotBlank(message = "Le code entreprise est obligatoire")
    @Column(nullable = false)
    private String companyCode;

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    @Column(nullable = false)
    private String companyName;

    @NotBlank(message = "Le code pays est obligatoire")
    @Column(nullable = false)
    private String countryCode;

    @NotBlank(message = "Le nom du pays est obligatoire")
    @Column(nullable = false)
    private String countryName;

    @NotNull(message = "La date de début est obligatoire")
    @Column(nullable = false)
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer rank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workbook_id", nullable = false)
    private Workbook workbook;
}
