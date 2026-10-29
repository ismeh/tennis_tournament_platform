package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.MemberTier;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.infrastructure.controller.dto.MemberRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MemberResponse;
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
class MemberWebMapperTest {

    @Autowired
    private MemberWebMapper mapper;

    @Nested
    class ToDomainTests {
        @Test
        void should_map_request_to_domain() {
            MemberRequest request = new MemberRequest(
                    "test@test.com",
                    "testuser",
                    "password123",
                    "M",
                    "ADVANCED"
            );

            Member domain = mapper.toDomain(request);

            assertThat(domain.getEmail()).isEqualTo("test@test.com");
            assertThat(domain.getPassword()).isEqualTo("password123");
            assertThat(domain.getId()).isNull();
            assertThat(domain.getRegisteredAt()).isNull();
        }

        @Test
        void should_ignore_id_and_registered_at() {
            MemberRequest request = new MemberRequest(
                    "a@test.com", "user", "pass", null, null
            );

            Member domain = mapper.toDomain(request);

            assertThat(domain.getId()).isNull();
            assertThat(domain.getRegisteredAt()).isNull();
        }
    }

    @Nested
    class ToResponseTests {
        @Test
        void should_map_domain_to_response() {
            UUID id = UUID.randomUUID();
            Member domain = Member.builder()
                    .id(id)
                    .email("test@test.com")
                    .username("testuser")
                    .gender("M")
                    .tier(MemberTier.ADVANCED)
                    .registeredAt(LocalDateTime.of(2025, 1, 1, 12, 0))
                    .build();

            MemberResponse response = mapper.toResponse(domain);

            assertThat(response.id()).isEqualTo(id);
            assertThat(response.email()).isEqualTo("test@test.com");
            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.gender()).isEqualTo("M");
            assertThat(response.tier()).isEqualTo("ADVANCED");
            assertThat(response.registeredAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 12, 0));
        }

        @Test
        void should_handle_null_fields() {
            Member domain = Member.builder()
                    .id(UUID.randomUUID())
                    .build();

            MemberResponse response = mapper.toResponse(domain);

            assertThat(response.email()).isNull();
            assertThat(response.username()).isNull();
            assertThat(response.gender()).isNull();
            assertThat(response.tier()).isNull();
        }
    }
}
