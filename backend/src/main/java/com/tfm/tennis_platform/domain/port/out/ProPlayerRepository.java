package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.ProPlayer;

import java.util.List;
import java.util.Optional;

public interface ProPlayerRepository {
    Optional<ProPlayer> findById(Integer id);
    Optional<ProPlayer> findByLicense(String license);
    List<ProPlayer> findTop10();
    List<ProPlayer> searchByQuery(String query);
    List<ProPlayer> search(String query, String gender, String category);
}
