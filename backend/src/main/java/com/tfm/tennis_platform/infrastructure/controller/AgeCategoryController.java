package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.service.AgeCategoryService;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
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
    public List<RefAgeCategoryEntity> getAll() {
        return ageCategoryService.getAll();
    }
}

