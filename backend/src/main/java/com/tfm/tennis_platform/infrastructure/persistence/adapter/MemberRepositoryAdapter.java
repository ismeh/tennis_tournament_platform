package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MemberMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public void updateTokenHash(UUID id, String tokenHash) {
        memberRepository.updateTokenHash(id, tokenHash);
    }
}
