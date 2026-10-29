package com.puntomatch.infrastructure.controller;

import com.puntomatch.application.services.MemberService;
import com.puntomatch.domain.exceptions.ResourceNotFoundException;
import com.puntomatch.domain.models.Member;
import com.puntomatch.infrastructure.controller.dto.MemberRequest;
import com.puntomatch.infrastructure.controller.dto.MemberResponse;
import com.puntomatch.infrastructure.controller.mapper.MemberWebMapper;
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
        Member member = memberService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Member", email));
        return ResponseEntity.ok(memberMapper.toResponse(member));
    }
}
