package com.puntomatch.infrastructure.controller;

import com.puntomatch.application.commands.AgeCategoryOutput;
import com.puntomatch.application.services.AgeCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/age-categories")
@RequiredArgsConstructor
public class AgeCategoryController {
    private final AgeCategoryService ageCategoryService;

    @GetMapping
    public List<AgeCategoryOutput> getAll() {
        return ageCategoryService.getAll();
    }
}

