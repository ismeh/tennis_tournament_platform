package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.contracts.MemberRepository;
import com.tfm.tennis_platform.domain.models.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Member register(Member member) {
        // Here we would encode password if not done, check duplicate email, etc.
        // For simplicity, we assume generic checks are done or handle exceptions
        return memberRepository.save(member);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public Optional<Member> findById(UUID id) {
        return memberRepository.findById(id);
    }
}
