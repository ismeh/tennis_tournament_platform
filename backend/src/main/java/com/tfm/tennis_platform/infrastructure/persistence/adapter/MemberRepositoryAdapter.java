package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.UmpireSearchResult;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MemberMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MemberRepositoryAdapter implements MemberRepository {

    private final JpaMemberRepository memberRepository;
    private final MemberMapper mapper;

    @Override
    public Member save(Member member) {
        return mapper.toDomain(memberRepository.save(mapper.toEntity(member)));
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<Member> findById(UUID id) {
        return memberRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Member> findByEmailConfirmationTokenHash(String tokenHash) {
        return memberRepository.findByEmailConfirmationTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void updateTokenHash(UUID id, String tokenHash) {
        memberRepository.updateTokenHash(id, tokenHash);
    }

    @Override
    public void updateEmailConfirmation(UUID id, boolean emailVerified, String tokenHash, LocalDateTime expiresAt) {
        memberRepository.updateEmailConfirmation(id, emailVerified, tokenHash, expiresAt);
    }

    @Override
    public void updatePersonId(UUID id, UUID personId) {
        memberRepository.updatePersonId(id, personId);
    }

    @Override
    public Optional<Member> findByEmailWithPersonId(String email) {
        return memberRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public void anonymize(UUID id, String anonymizedEmail) {
        memberRepository.anonymize(id, anonymizedEmail);
    }

    @Override
    public void updatePrivacyConsent(UUID id, boolean accepted, LocalDateTime acceptedAt, String version) {
        memberRepository.updatePrivacyConsent(id, accepted, acceptedAt, version);
    }

    @Override
    public void updateTermsConsent(UUID id, boolean accepted, LocalDateTime acceptedAt, String version) {
        memberRepository.updateTermsConsent(id, accepted, acceptedAt, version);
    }

    @Override
    public Optional<Member> findByRole(UserRole role) {
        return memberRepository.findFirstByRole(role).map(mapper::toDomain);
    }

    @Override
    public List<Member> findAllByRole(UserRole role) {
        return memberRepository.findAllByRole(role).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Member> searchUmpiresByQuery(String query) {
        return memberRepository.searchByRoleAndQuery(UserRole.UMPIRE.name(), query).stream()
                .map(p -> Member.builder()
                        .id(p.getId())
                        .email(p.getEmail())
                        .build())
                .toList();
    }

    @Override
    public List<UmpireSearchResult> searchUmpiresWithPersonData(String query) {
        if (query == null || query.trim().isEmpty()) {
            return memberRepository.findAllByRoleWithPersonData(UserRole.UMPIRE.name()).stream()
                    .map(p -> UmpireSearchResult.builder()
                            .id(p.getId())
                            .email(p.getEmail())
                            .firstName(p.getFirstName())
                            .lastName(p.getLastName())
                            .build())
                    .toList();
        }
        return memberRepository.searchByRoleAndQuery(UserRole.UMPIRE.name(), query.trim()).stream()
                .map(p -> UmpireSearchResult.builder()
                        .id(p.getId())
                        .email(p.getEmail())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .build())
                .toList();
    }
}
