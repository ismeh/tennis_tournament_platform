package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.MemberService;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.infrastructure.controller.dto.MemberRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MemberResponse;
import com.tfm.tennis_platform.infrastructure.controller.mapper.MemberWebMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberService memberService;
    @Mock
    private MemberWebMapper memberMapper;
    @InjectMocks
    private MemberController controller;

    @Test
    void should_register_member() {
        MemberRequest request = new MemberRequest("a@test.com", "user", "pass", "M", "GOLD");
        Member domain = Member.builder().email("a@test.com").role(UserRole.PLAYER).build();
        Member saved = Member.builder().id(UUID.randomUUID()).email("a@test.com").role(UserRole.PLAYER).build();
        MemberResponse response = new MemberResponse(saved.getId(), "a@test.com", null, null, null, null);

        when(memberMapper.toDomain(request)).thenReturn(domain);
        when(memberService.register(domain)).thenReturn(saved);
        when(memberMapper.toResponse(saved)).thenReturn(response);

        ResponseEntity<MemberResponse> result = controller.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().email()).isEqualTo("a@test.com");
        verify(memberService).register(domain);
    }

    @Test
    void should_get_by_email() {
        String email = "a@test.com";
        Member member = Member.builder().id(UUID.randomUUID()).email(email).role(UserRole.PLAYER).build();
        MemberResponse response = new MemberResponse(member.getId(), email, null, null, null, null);

        when(memberService.findByEmail(email)).thenReturn(Optional.of(member));
        when(memberMapper.toResponse(member)).thenReturn(response);

        ResponseEntity<MemberResponse> result = controller.getByEmail(email);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().email()).isEqualTo(email);
    }

    @Test
    void should_throw_when_email_not_found() {
        when(memberService.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getByEmail("missing@test.com"))
                .isInstanceOf(com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException.class);
    }
}
