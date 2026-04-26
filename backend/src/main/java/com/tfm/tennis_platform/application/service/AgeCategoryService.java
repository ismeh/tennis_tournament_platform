package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.RefAgeCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgeCategoryService {
    private final RefAgeCategoryRepository refAgeCategoryRepository;

    public List<RefAgeCategoryEntity> getAll() {
        return refAgeCategoryRepository.findAll();
    }
}

