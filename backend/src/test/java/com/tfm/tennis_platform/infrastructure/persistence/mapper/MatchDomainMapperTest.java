package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaCourtRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaDrawRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaProPlayerRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MatchDomainMapperTest {

    @Test
    void should_map_incomplete_matches_without_loading_professional_data() {
        JpaProPlayerRepository proPlayerRepository = mock(JpaProPlayerRepository.class);
        MatchDomainMapper mapper = new MatchDomainMapper(
                mock(JpaDrawRepository.class),
                mock(JpaInscriptionRepository.class),
                mock(JpaMatchRepository.class),
                mock(JpaCourtRepository.class),
                proPlayerRepository
        );
        UUID matchId = UUID.randomUUID();
        MatchEntity matchEntity = MatchEntity.builder()
                .id(matchId)
                .roundNumber(1)
                .build();

        List<Match> matches = mapper.toDomainList(List.of(matchEntity));

        assertEquals(1, matches.size());
        assertEquals(matchId, matches.getFirst().getId());
        assertNull(matches.getFirst().getFirstInscriptionId());
        assertNull(matches.getFirst().getSecondInscriptionId());
        assertNull(matches.getFirst().getWinnerId());
        verifyNoInteractions(proPlayerRepository);
    }
}
