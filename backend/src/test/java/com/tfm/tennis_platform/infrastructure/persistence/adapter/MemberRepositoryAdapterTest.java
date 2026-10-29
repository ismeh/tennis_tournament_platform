package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.UmpireSearchResult;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MemberMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.UmpireSearchProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberRepositoryAdapterTest {

    @Mock
    private JpaMemberRepository memberRepository;
    @Mock
    private MemberMapper mapper;
    @InjectMocks
    private MemberRepositoryAdapter adapter;

    @Test
    void should_save_member() {
        UUID id = UUID.randomUUID();
        Member domain = Member.builder().id(id).email("a@test.com").role(UserRole.PLAYER).build();
        MemberEntity entity = MemberEntity.builder().id(id).email("a@test.com").role(UserRole.PLAYER).build();
        MemberEntity saved = MemberEntity.builder().id(id).email("a@test.com").role(UserRole.PLAYER).build();
        Member mapped = Member.builder().id(id).email("a@test.com").role(UserRole.PLAYER).build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(memberRepository.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(mapped);

        Member result = adapter.save(domain);

        assertThat(result).isEqualTo(mapped);
    }

    @Test
    void should_find_by_email() {
        MemberEntity entity = MemberEntity.builder().id(UUID.randomUUID()).email("a@test.com").build();
        Member mapped = Member.builder().id(entity.getId()).email("a@test.com").build();

        when(memberRepository.findByEmail("a@test.com")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mapped);

        Optional<Member> result = adapter.findByEmail("a@test.com");

        assertThat(result).contains(mapped);
    }

    @Test
    void should_return_empty_when_email_not_found() {
        when(memberRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThat(adapter.findByEmail("missing@test.com")).isEmpty();
    }

    @Test
    void should_find_by_id() {
        UUID id = UUID.randomUUID();
        MemberEntity entity = MemberEntity.builder().id(id).build();
        Member mapped = Member.builder().id(id).build();

        when(memberRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mapped);

        assertThat(adapter.findById(id)).contains(mapped);
    }

    @Test
    void should_find_by_email_confirmation_token() {
        String token = "abc123";
        MemberEntity entity = MemberEntity.builder().id(UUID.randomUUID()).build();
        Member mapped = Member.builder().id(entity.getId()).build();

        when(memberRepository.findByEmailConfirmationTokenHash(token)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mapped);

        assertThat(adapter.findByEmailConfirmationTokenHash(token)).contains(mapped);
    }

    @Test
    void should_update_token_hash() {
        UUID id = UUID.randomUUID();
        when(memberRepository.updateTokenHash(id, "hash")).thenReturn(1);

        adapter.updateTokenHash(id, "hash");

        verify(memberRepository).updateTokenHash(id, "hash");
    }

    @Test
    void should_update_email_confirmation() {
        UUID id = UUID.randomUUID();
        LocalDateTime expires = LocalDateTime.now().plusDays(1);
        when(memberRepository.updateEmailConfirmation(id, true, "token", expires)).thenReturn(1);

        adapter.updateEmailConfirmation(id, true, "token", expires);

        verify(memberRepository).updateEmailConfirmation(id, true, "token", expires);
    }

    @Test
    void should_update_person_id() {
        UUID id = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        when(memberRepository.updatePersonId(id, personId)).thenReturn(1);

        adapter.updatePersonId(id, personId);

        verify(memberRepository).updatePersonId(id, personId);
    }

    @Test
    void should_anonymize() {
        UUID id = UUID.randomUUID();
        when(memberRepository.anonymize(id, "anon@test.com")).thenReturn(1);

        adapter.anonymize(id, "anon@test.com");

        verify(memberRepository).anonymize(id, "anon@test.com");
    }

    @Test
    void should_update_privacy_consent() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        when(memberRepository.updatePrivacyConsent(id, true, now, "v1")).thenReturn(1);

        adapter.updatePrivacyConsent(id, true, now, "v1");

        verify(memberRepository).updatePrivacyConsent(id, true, now, "v1");
    }

    @Test
    void should_find_by_role() {
        MemberEntity entity = MemberEntity.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).build();
        Member mapped = Member.builder().id(entity.getId()).role(UserRole.ADMIN).build();

        when(memberRepository.findFirstByRole(UserRole.ADMIN)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mapped);

        assertThat(adapter.findByRole(UserRole.ADMIN)).contains(mapped);
    }

    @Test
    void should_find_by_email_with_person_id() {
        MemberEntity entity = MemberEntity.builder().id(UUID.randomUUID()).email("a@test.com").build();
        Member mapped = Member.builder().id(entity.getId()).email("a@test.com").build();

        when(memberRepository.findByEmail("a@test.com")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mapped);

        assertThat(adapter.findByEmailWithPersonId("a@test.com")).contains(mapped);
    }

    @Test
    void should_update_terms_consent() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        when(memberRepository.updateTermsConsent(id, true, now, "v1")).thenReturn(1);

        adapter.updateTermsConsent(id, true, now, "v1");

        verify(memberRepository).updateTermsConsent(id, true, now, "v1");
    }

    @Test
    void should_find_all_by_role() {
        MemberEntity e1 = MemberEntity.builder().id(UUID.randomUUID()).role(UserRole.UMPIRE).build();
        MemberEntity e2 = MemberEntity.builder().id(UUID.randomUUID()).role(UserRole.UMPIRE).build();
        Member m1 = Member.builder().id(e1.getId()).role(UserRole.UMPIRE).build();
        Member m2 = Member.builder().id(e2.getId()).role(UserRole.UMPIRE).build();

        when(memberRepository.findAllByRole(UserRole.UMPIRE)).thenReturn(List.of(e1, e2));
        when(mapper.toDomain(e1)).thenReturn(m1);
        when(mapper.toDomain(e2)).thenReturn(m2);

        List<Member> result = adapter.findAllByRole(UserRole.UMPIRE);

        assertThat(result).hasSize(2).containsExactly(m1, m2);
    }

    @Test
    void should_search_umpires_by_query() {
        UmpireSearchProjection projection = mock(UmpireSearchProjection.class);
        UUID id = UUID.randomUUID();
        when(projection.getId()).thenReturn(id);
        when(projection.getEmail()).thenReturn("ump@test.com");

        when(memberRepository.searchByRoleAndQuery("UMPIRE", "test")).thenReturn(List.of(projection));

        List<Member> result = adapter.searchUmpiresByQuery("test");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(id);
        assertThat(result.getFirst().getEmail()).isEqualTo("ump@test.com");
    }

    @Test
    void should_search_umpires_with_person_data_when_query_is_null() {
        UmpireSearchProjection projection = mock(UmpireSearchProjection.class);
        when(projection.getId()).thenReturn(UUID.randomUUID());
        when(projection.getEmail()).thenReturn("ump@test.com");
        when(projection.getFirstName()).thenReturn("John");
        when(projection.getLastName()).thenReturn("Doe");

        when(memberRepository.findAllByRoleWithPersonData("UMPIRE")).thenReturn(List.of(projection));

        List<UmpireSearchResult> result = adapter.searchUmpiresWithPersonData(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getFirstName()).isEqualTo("John");
    }

    @Test
    void should_search_umpires_with_person_data_when_query_is_blank() {
        when(memberRepository.findAllByRoleWithPersonData("UMPIRE")).thenReturn(List.of());

        List<UmpireSearchResult> result = adapter.searchUmpiresWithPersonData("   ");

        assertThat(result).isEmpty();
    }

    @Test
    void should_search_umpires_with_person_data_when_query_is_present() {
        UmpireSearchProjection projection = mock(UmpireSearchProjection.class);
        when(projection.getId()).thenReturn(UUID.randomUUID());
        when(projection.getEmail()).thenReturn("ump@test.com");
        when(projection.getFirstName()).thenReturn("Jane");
        when(projection.getLastName()).thenReturn("Smith");

        when(memberRepository.searchByRoleAndQuery("UMPIRE", "Jane")).thenReturn(List.of(projection));

        List<UmpireSearchResult> result = adapter.searchUmpiresWithPersonData("Jane");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getFirstName()).isEqualTo("Jane");
    }

    @Test
    void should_search_by_roles_with_person_data_when_query_is_null() {
        UmpireSearchProjection projection = mock(UmpireSearchProjection.class);
        when(projection.getId()).thenReturn(UUID.randomUUID());
        when(projection.getEmail()).thenReturn("admin@test.com");
        when(projection.getFirstName()).thenReturn("Admin");
        when(projection.getLastName()).thenReturn("User");

        when(memberRepository.findAllByRolesWithPersonData(List.of("ADMIN", "UMPIRE"))).thenReturn(List.of(projection));

        List<UmpireSearchResult> result = adapter.searchByRolesWithPersonData(List.of(UserRole.ADMIN, UserRole.UMPIRE), null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEmail()).isEqualTo("admin@test.com");
    }

    @Test
    void should_search_by_roles_with_person_data_when_query_is_present() {
        UmpireSearchProjection projection = mock(UmpireSearchProjection.class);
        when(projection.getId()).thenReturn(UUID.randomUUID());
        when(projection.getEmail()).thenReturn("adm@test.com");
        when(projection.getFirstName()).thenReturn("Admin");
        when(projection.getLastName()).thenReturn("User");

        when(memberRepository.searchByRolesAndQuery(List.of("ADMIN"), "Admin")).thenReturn(List.of(projection));

        List<UmpireSearchResult> result = adapter.searchByRolesWithPersonData(List.of(UserRole.ADMIN), "Admin");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getFirstName()).isEqualTo("Admin");
    }
}
