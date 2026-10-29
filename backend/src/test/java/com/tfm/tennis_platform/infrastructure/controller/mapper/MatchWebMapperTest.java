package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MatchWebMapperTest {

    @Autowired
    private MatchWebMapper mapper;

    @Nested
    class ToResponseTests {
        @Test
        void should_map_domain_to_response() {
            UUID id = UUID.randomUUID();
            UUID firstId = UUID.randomUUID();
            UUID secondId = UUID.randomUUID();
            UUID winnerId = UUID.randomUUID();

            Match domain = Match.builder()
                    .id(id)
                    .firstInscription(com.tfm.tennis_platform.domain.models.Inscription.builder().id(firstId).build())
                    .secondInscription(com.tfm.tennis_platform.domain.models.Inscription.builder().id(secondId).build())
                    .winner(com.tfm.tennis_platform.domain.models.Inscription.builder().id(winnerId).build())
                    .roundNumber(1)
                    .bracketPosition(0)
                    .scheduledAt(LocalDateTime.of(2025, 6, 15, 10, 0))
                    .scheduleTimeType(ScheduleTimeType.EXACT)
                    .courtId(UUID.randomUUID())
                    .court("Court 1")
                    .result("6-3 7-5")
                    .status(MatchStatus.COMPLETED)
                    .build();

            MatchResponse response = mapper.toResponse(domain);

            assertThat(response.id()).isEqualTo(id);
            assertThat(response.firstInscriptionId()).isEqualTo(firstId);
            assertThat(response.secondInscriptionId()).isEqualTo(secondId);
            assertThat(response.winnerId()).isEqualTo(winnerId);
            assertThat(response.roundNumber()).isEqualTo(1);
            assertThat(response.bracketPosition()).isEqualTo(0);
            assertThat(response.scheduledAt()).isEqualTo(LocalDateTime.of(2025, 6, 15, 10, 0));
            assertThat(response.scheduleTimeType()).isEqualTo("EXACT");
            assertThat(response.court()).isEqualTo("Court 1");
            assertThat(response.result()).isEqualTo("6-3 7-5");
            assertThat(response.status()).isEqualTo("COMPLETED");
        }

        @Test
        void should_handle_null_inscriptions() {
            Match domain = Match.builder()
                    .id(UUID.randomUUID())
                    .build();

            MatchResponse response = mapper.toResponse(domain);

            assertThat(response.firstInscriptionId()).isNull();
            assertThat(response.secondInscriptionId()).isNull();
            assertThat(response.winnerId()).isNull();
        }

        @Test
        void should_handle_null_status() {
            Match domain = Match.builder()
                    .id(UUID.randomUUID())
                    .status(null)
                    .build();

            MatchResponse response = mapper.toResponse(domain);

            assertThat(response.status()).isNull();
        }
    }

    @Nested
    class ToDomainTests {
        @Test
        void should_map_response_to_domain() {
            UUID firstId = UUID.randomUUID();
            UUID secondId = UUID.randomUUID();
            UUID winnerId = UUID.randomUUID();

            MatchResponse response = new MatchResponse(
                    UUID.randomUUID(),
                    firstId,
                    secondId,
                    winnerId,
                    2,
                    1,
                    LocalDateTime.of(2025, 6, 15, 14, 0),
                    "NOT_BEFORE",
                    null,
                    "Court 2",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "PENDING"
            );

            Match domain = mapper.toDomain(response);

            assertThat(domain.getFirstInscription().getId()).isEqualTo(firstId);
            assertThat(domain.getSecondInscription().getId()).isEqualTo(secondId);
            assertThat(domain.getWinner().getId()).isEqualTo(winnerId);
            assertThat(domain.getRoundNumber()).isEqualTo(2);
            assertThat(domain.getBracketPosition()).isEqualTo(1);
            assertThat(domain.getCourt()).isEqualTo("Court 2");
            assertThat(domain.getStatus()).isEqualTo(MatchStatus.PENDING);
        }

        @Test
        void should_handle_null_status_string() {
            MatchResponse response = new MatchResponse(
                    UUID.randomUUID(), null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null
            );

            Match domain = mapper.toDomain(response);

            assertThat(domain.getStatus()).isNull();
        }

        @Test
        void should_ignore_draw_id() {
            MatchResponse response = new MatchResponse(
                    UUID.randomUUID(), null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, "PENDING"
            );

            Match domain = mapper.toDomain(response);

            assertThat(domain.getDrawId()).isNull();
        }
    }
}
