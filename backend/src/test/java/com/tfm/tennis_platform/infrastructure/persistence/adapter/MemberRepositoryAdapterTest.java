package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MemberMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}
