package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.MemberTier;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void register_saves_and_returns_member() {
        Member member = Member.builder()
                .email("test@example.com")
                .role(UserRole.PLAYER)
                .tier(MemberTier.FREE)
                .registeredAt(LocalDateTime.now())
                .build();
        when(memberRepository.save(member)).thenReturn(member);

        Member result = memberService.register(member);

        assertEquals(member, result);
        verify(memberRepository).save(member);
    }

    @Test
    void findByEmail_delegates_to_repository() {
        UUID id = UUID.randomUUID();
        Member member = Member.builder().id(id).email("test@example.com").role(UserRole.PLAYER).build();
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));

        Optional<Member> result = memberService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void findByEmail_returns_empty_when_not_found() {
        when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        Optional<Member> result = memberService.findByEmail("unknown@example.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void findById_delegates_to_repository() {
        UUID id = UUID.randomUUID();
        Member member = Member.builder().id(id).email("test@example.com").role(UserRole.PLAYER).build();
        when(memberRepository.findById(id)).thenReturn(Optional.of(member));

        Optional<Member> result = memberService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void findById_returns_empty_when_not_found() {
        UUID id = UUID.randomUUID();
        when(memberRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Member> result = memberService.findById(id);

        assertTrue(result.isEmpty());
    }
}
