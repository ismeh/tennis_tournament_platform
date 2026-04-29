package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.dto.AgeCategoryOutput;
import com.tfm.tennis_platform.domain.port.out.AgeCategoryRefRepository;
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

