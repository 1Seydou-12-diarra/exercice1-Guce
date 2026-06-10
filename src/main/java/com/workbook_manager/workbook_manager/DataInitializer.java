package com.workbook_manager.workbook_manager;

import com.workbook_manager.workbook_manager.entite.Workbook;
import com.workbook_manager.workbook_manager.entite.Workplace;
import com.workbook_manager.workbook_manager.repository.WorkBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final WorkBookRepository workbookRepository;

    @Override
    public void run(String... args) {

        // Workbook 1
        Workbook wb1 = new Workbook();
        wb1.setFirstName("Alice");
        wb1.setLastName("Martin");
        wb1.setBirthdate(LocalDate.of(1990, 5, 14));
        wb1.setPassportNumber("FR1234567");
        wb1.setEmail("alice.martin@example.com");

        Workplace wp1 = new Workplace();
        wp1.setCompanyCode("ACME");
        wp1.setCompanyName("Acme Corporation");
        wp1.setCountryCode("FR");
        wp1.setCountryName("France");
        wp1.setStartDate(LocalDate.of(2015, 1, 1));
        wp1.setEndDate(LocalDate.of(2019, 12, 31));
        wp1.setCurrent(false);
        wp1.setRank(1);
        wp1.setWorkbook(wb1);

        Workplace wp2 = new Workplace();
        wp2.setCompanyCode("TECH");
        wp2.setCompanyName("TechSolutions SAS");
        wp2.setCountryCode("FR");
        wp2.setCountryName("France");
        wp2.setStartDate(LocalDate.of(2020, 1, 15));
        wp2.setEndDate(null);
        wp2.setCurrent(true);
        wp2.setRank(2);
        wp2.setWorkbook(wb1);

        wb1.getWorkplaces().add(wp1);
        wb1.getWorkplaces().add(wp2);
        workbookRepository.save(wb1);
    }
}