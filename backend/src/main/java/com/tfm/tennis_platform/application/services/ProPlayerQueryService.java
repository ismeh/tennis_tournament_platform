package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.domain.port.out.ProPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProPlayerQueryService {

    private final ProPlayerRepository proPlayerRepository;

    @Transactional(readOnly = true)
    public List<ProPlayer> search(String query) {
        if (query == null || query.isBlank()) {
            return proPlayerRepository.findTop10();
        }

        return proPlayerRepository.searchByQuery(query.trim());
    }
}
