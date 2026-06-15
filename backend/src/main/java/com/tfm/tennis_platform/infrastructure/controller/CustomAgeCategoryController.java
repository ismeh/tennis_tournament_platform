package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.commands.AgeCategoryOutput;
import com.tfm.tennis_platform.application.commands.CustomAgeCategoryRequest;
import com.tfm.tennis_platform.application.services.CustomAgeCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/custom-age-categories")
@RequiredArgsConstructor
public class CustomAgeCategoryController {

    private final CustomAgeCategoryService customAgeCategoryService;

    @GetMapping
    public List<AgeCategoryOutput> getMyCategories(Principal principal) {
        return customAgeCategoryService.getMyCategories(principal.getName());
    }

    @PostMapping
    public ResponseEntity<AgeCategoryOutput> create(
            @RequestBody CustomAgeCategoryRequest request,
            Principal principal) {
        AgeCategoryOutput created = customAgeCategoryService.create(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public AgeCategoryOutput update(
            @PathVariable Integer id,
            @RequestBody CustomAgeCategoryRequest request,
            Principal principal) {
        return customAgeCategoryService.update(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Principal principal) {
        customAgeCategoryService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
