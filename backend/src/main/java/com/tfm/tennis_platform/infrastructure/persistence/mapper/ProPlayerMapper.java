package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ProPlayerEntity;
import org.springframework.stereotype.Component;

@Component
public class ProPlayerMapper {

    public ProPlayer toDomain(ProPlayerEntity entity) {
        if (entity == null) {
            return null;
        }

        String normalizedName = normalize(entity.getName());
        NameParts nameParts = splitName(normalizedName);

        return ProPlayer.builder()
                .id(entity.getId())
                .license(normalize(entity.getLicense()))
                .fullName(normalizedName)
                .firstName(nameParts.firstName())
                .lastName(nameParts.lastName())
                .rankingPosition(entity.getRankingPosition())
                .ageCategory(normalize(entity.getAgeCategory()))
                .clubName(normalize(entity.getClubName()))
                .birthDate(entity.getBirthDate())
                .gender(normalize(entity.getGender()))
                .build();
    }

    private NameParts splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new NameParts("", null);
        }

        String[] parts = fullName.split(",", 2);
        if (parts.length < 2) {
            return new NameParts(fullName.trim(), null);
        }

        String lastName = normalize(parts[0]);
        String firstName = normalize(parts[1]);
        return new NameParts(firstName == null ? "" : firstName, lastName);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private record NameParts(String firstName, String lastName) {
    }
}
