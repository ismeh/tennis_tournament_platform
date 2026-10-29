package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.AgeCategoryRef;

import java.util.List;

public interface AgeCategoryRefRepository {
    List<AgeCategoryRef> findAll();
}
