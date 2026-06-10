package com.workbook_manager.workbook_manager.controller;

import org.springframework.web.bind.annotation.GetMapping;

public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/workbooks";
    }
}
