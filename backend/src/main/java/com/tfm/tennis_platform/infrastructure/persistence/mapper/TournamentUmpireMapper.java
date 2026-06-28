package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.TournamentUmpire;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentUmpireEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TournamentUmpireMapper {

    private final JpaTournamentRepository tournamentRepository;
    private final JpaMemberRepository memberRepository;
    private final JpaPersonRepository personRepository;

    public TournamentUmpire toDomain(TournamentUmpireEntity entity) {
        if (entity == null) {
            return null;
        }

        String email = null;
        String firstName = null;
        String lastName = null;

        if (entity.getUmpire() != null) {
            email = entity.getUmpire().getEmail();
            if (entity.getUmpire().getPersonId() != null) {
                PersonEntity person = personRepository.findById(entity.getUmpire().getPersonId()).orElse(null);
                if (person != null) {
                    firstName = person.getFirstName();
                    lastName = person.getLastName();
                }
            }
        }

        return TournamentUmpire.builder()
                .id(entity.getId())
                .tournamentId(entity.getTournament() != null ? entity.getTournament().getId() : null)
                .umpireId(entity.getUmpire() != null ? entity.getUmpire().getId() : null)
                .umpireEmail(email)
                .umpireFirstName(firstName)
                .umpireLastName(lastName)
                .assignedAt(entity.getAssignedAt())
                .build();
    }

    public TournamentUmpireEntity toEntity(TournamentUmpire domain) {
        if (domain == null) {
            return null;
        }

        return TournamentUmpireEntity.builder()
                .id(domain.getId())
                .tournament(tournamentRepository.getReferenceById(domain.getTournamentId()))
                .umpire(memberRepository.getReferenceById(domain.getUmpireId()))
                .assignedAt(domain.getAssignedAt())
                .build();
    }
}
