package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.commands.AgeCategoryOutput;
import com.tfm.tennis_platform.application.services.AgeCategoryService;
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

