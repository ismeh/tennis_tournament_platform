package com.puntomatch.application.services;

import com.puntomatch.application.commands.AgeCategoryOutput;
import com.puntomatch.domain.port.out.AgeCategoryRefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgeCategoryService {
    private final AgeCategoryRefRepository ageCategoryRefRepository;

    public List<AgeCategoryOutput> getAll() {
        return ageCategoryRefRepository.findAll().stream()
                .map(category -> new AgeCategoryOutput(category.getId(), category.getCategory()))
                .toList();
    }
}

