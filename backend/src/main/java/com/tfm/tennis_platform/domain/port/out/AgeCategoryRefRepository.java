package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.AgeCategoryRef;

import java.util.List;

public interface AgeCategoryRefRepository {
    List<AgeCategoryRef> findAll();
}
