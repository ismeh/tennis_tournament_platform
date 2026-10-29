package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.ProPlayer;

import java.util.List;
import java.util.Optional;

public interface ProPlayerRepository {
    Optional<ProPlayer> findById(Integer id);
    Optional<ProPlayer> findByLicense(String license);
    List<ProPlayer> findTop10();
    List<ProPlayer> searchByQuery(String query);
}
