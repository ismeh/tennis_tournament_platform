package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.MemberService;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.infrastructure.controller.dto.MemberRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MemberResponse;
import com.tfm.tennis_platform.infrastructure.controller.mapper.MemberWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberWebMapper memberMapper;

    @PostMapping
    public ResponseEntity<MemberResponse> register(@RequestBody MemberRequest request) {
        Member member = memberMapper.toDomain(request);
        Member saved = memberService.register(member);
        return ResponseEntity.ok(memberMapper.toResponse(saved));
    }

    @GetMapping("/{email}")
    public ResponseEntity<MemberResponse> getByEmail(@PathVariable String email) {
        return memberService.findByEmail(email)
                .map(member -> ResponseEntity.ok(memberMapper.toResponse(member)))
                .orElse(ResponseEntity.notFound().build());
    }
}
