package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubEntity;
import org.springframework.stereotype.Component;

@Component
public class ClubDomainMapper {

    public Club toDomain(ClubEntity entity) {
        if (entity == null) {
            return null;
        }
        return Club.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .tagline(entity.getTagline())
                .description(entity.getDescription())
                .logoUrl(entity.getLogoUrl())
                .bannerUrl(entity.getBannerUrl())
                .primaryColor(entity.getPrimaryColor())
                .secondaryColor(entity.getSecondaryColor())
                .city(entity.getCity())
                .country(entity.getCountry())
                .contactEmail(entity.getContactEmail())
                .phone(entity.getPhone())
                .website(entity.getWebsite())
                .instagramUrl(entity.getInstagramUrl())
                .facebookUrl(entity.getFacebookUrl())
                .xUrl(entity.getXUrl())
                .foundedYear(entity.getFoundedYear())
                .publicPageEnabled(entity.isPublicPageEnabled())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ClubEntity toEntity(Club domain) {
        if (domain == null) {
            return null;
        }
        return ClubEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .slug(domain.getSlug())
                .tagline(domain.getTagline())
                .description(domain.getDescription())
                .logoUrl(domain.getLogoUrl())
                .bannerUrl(domain.getBannerUrl())
                .primaryColor(domain.getPrimaryColor())
                .secondaryColor(domain.getSecondaryColor())
                .city(domain.getCity())
                .country(domain.getCountry())
                .contactEmail(domain.getContactEmail())
                .phone(domain.getPhone())
                .website(domain.getWebsite())
                .instagramUrl(domain.getInstagramUrl())
                .facebookUrl(domain.getFacebookUrl())
                .xUrl(domain.getXUrl())
                .foundedYear(domain.getFoundedYear())
                .publicPageEnabled(domain.isPublicPageEnabled())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
