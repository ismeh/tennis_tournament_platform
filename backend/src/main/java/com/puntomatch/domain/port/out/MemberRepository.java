package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.Member;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findByEmail(String email);
    Optional<Member> findById(UUID id);
    void updateTokenHash(UUID id, String tokenHash);
    void updatePersonId(UUID id, UUID personId);
    Optional<Member> findByEmailWithPersonId(String email);
}
