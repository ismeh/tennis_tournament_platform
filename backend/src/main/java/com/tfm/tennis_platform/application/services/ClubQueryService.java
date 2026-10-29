package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.domain.port.out.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubQueryService {

    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public List<Club> search(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        return clubRepository.findByNameContaining(query.trim());
    }

    @Transactional
    public Club findOrCreate(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        return clubRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> clubRepository.save(Club.builder().name(trimmed).build()));
    }
}
