package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.TournamentUmpire;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentUmpireEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentUmpireMapperTest {

    @Mock
    private JpaTournamentRepository tournamentRepository;
    @Mock
    private JpaMemberRepository memberRepository;
    @Mock
    private JpaPersonRepository personRepository;
    @InjectMocks
    private TournamentUmpireMapper mapper;

    @Nested
    class ToDomainTests {
        @Test
        void should_return_null_when_entity_is_null() {
            assertThat(mapper.toDomain(null)).isNull();
        }

        @Test
        void should_map_with_null_umpire() {
            TournamentEntity tournament = TournamentEntity.builder().id(UUID.randomUUID()).build();
            TournamentUmpireEntity entity = TournamentUmpireEntity.builder()
                    .id(UUID.randomUUID())
                    .tournament(tournament)
                    .umpire(null)
                    .assignedAt(LocalDateTime.now())
                    .build();

            TournamentUmpire result = mapper.toDomain(entity);

            assertThat(result.getId()).isEqualTo(entity.getId());
            assertThat(result.getTournamentId()).isEqualTo(tournament.getId());
            assertThat(result.getUmpireId()).isNull();
            assertThat(result.getUmpireEmail()).isNull();
            assertThat(result.getUmpireFirstName()).isNull();
            assertThat(result.getUmpireLastName()).isNull();
            assertThat(result.getAssignedAt()).isEqualTo(entity.getAssignedAt());
        }

        @Test
        void should_map_with_umpire_but_null_personId() {
            MemberEntity umpire = MemberEntity.builder()
                    .id(UUID.randomUUID())
                    .email("umpire@example.com")
                    .personId(null)
                    .build();
            TournamentEntity tournament = TournamentEntity.builder().id(UUID.randomUUID()).build();
            TournamentUmpireEntity entity = TournamentUmpireEntity.builder()
                    .id(UUID.randomUUID())
                    .tournament(tournament)
                    .umpire(umpire)
                    .assignedAt(LocalDateTime.now())
                    .build();

            TournamentUmpire result = mapper.toDomain(entity);

            assertThat(result.getUmpireId()).isEqualTo(umpire.getId());
            assertThat(result.getUmpireEmail()).isEqualTo("umpire@example.com");
            assertThat(result.getUmpireFirstName()).isNull();
            assertThat(result.getUmpireLastName()).isNull();
        }

        @Test
        void should_map_with_umpire_and_person_not_found() {
            UUID personId = UUID.randomUUID();
            MemberEntity umpire = MemberEntity.builder()
                    .id(UUID.randomUUID())
                    .email("umpire@example.com")
                    .personId(personId)
                    .build();
            TournamentEntity tournament = TournamentEntity.builder().id(UUID.randomUUID()).build();
            TournamentUmpireEntity entity = TournamentUmpireEntity.builder()
                    .id(UUID.randomUUID())
                    .tournament(tournament)
                    .umpire(umpire)
                    .assignedAt(LocalDateTime.now())
                    .build();

            when(personRepository.findById(personId)).thenReturn(Optional.empty());

            TournamentUmpire result = mapper.toDomain(entity);

            assertThat(result.getUmpireEmail()).isEqualTo("umpire@example.com");
            assertThat(result.getUmpireFirstName()).isNull();
            assertThat(result.getUmpireLastName()).isNull();
        }

        @Test
        void should_map_with_umpire_and_person_found() {
            UUID personId = UUID.randomUUID();
            MemberEntity umpire = MemberEntity.builder()
                    .id(UUID.randomUUID())
                    .email("umpire@example.com")
                    .personId(personId)
                    .build();
            PersonEntity person = PersonEntity.builder()
                    .id(personId)
                    .firstName("Carlos")
                    .lastName("Alcaraz")
                    .build();
            TournamentEntity tournament = TournamentEntity.builder().id(UUID.randomUUID()).build();
            TournamentUmpireEntity entity = TournamentUmpireEntity.builder()
                    .id(UUID.randomUUID())
                    .tournament(tournament)
                    .umpire(umpire)
                    .assignedAt(LocalDateTime.now())
                    .build();

            when(personRepository.findById(personId)).thenReturn(Optional.of(person));

            TournamentUmpire result = mapper.toDomain(entity);

            assertThat(result.getUmpireEmail()).isEqualTo("umpire@example.com");
            assertThat(result.getUmpireFirstName()).isEqualTo("Carlos");
            assertThat(result.getUmpireLastName()).isEqualTo("Alcaraz");
        }

        @Test
        void should_map_with_null_tournament() {
            MemberEntity umpire = MemberEntity.builder()
                    .id(UUID.randomUUID())
                    .email("umpire@example.com")
                    .personId(null)
                    .build();
            TournamentUmpireEntity entity = TournamentUmpireEntity.builder()
                    .id(UUID.randomUUID())
                    .tournament(null)
                    .umpire(umpire)
                    .assignedAt(LocalDateTime.now())
                    .build();

            TournamentUmpire result = mapper.toDomain(entity);

            assertThat(result.getTournamentId()).isNull();
            assertThat(result.getUmpireId()).isEqualTo(umpire.getId());
        }
    }

    @Nested
    class ToEntityTests {
        @Test
        void should_return_null_when_domain_is_null() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        void should_map_domain_to_entity() {
            UUID tournamentId = UUID.randomUUID();
            UUID umpireId = UUID.randomUUID();
            TournamentEntity tournamentRef = TournamentEntity.builder().id(tournamentId).build();
            MemberEntity umpireRef = MemberEntity.builder().id(umpireId).build();

            when(tournamentRepository.getReferenceById(tournamentId)).thenReturn(tournamentRef);
            when(memberRepository.getReferenceById(umpireId)).thenReturn(umpireRef);

            TournamentUmpire domain = TournamentUmpire.builder()
                    .id(UUID.randomUUID())
                    .tournamentId(tournamentId)
                    .umpireId(umpireId)
                    .assignedAt(LocalDateTime.now())
                    .build();

            TournamentUmpireEntity result = mapper.toEntity(domain);

            assertThat(result.getId()).isEqualTo(domain.getId());
            assertThat(result.getTournament()).isEqualTo(tournamentRef);
            assertThat(result.getUmpire()).isEqualTo(umpireRef);
            assertThat(result.getAssignedAt()).isEqualTo(domain.getAssignedAt());
        }
    }
}
