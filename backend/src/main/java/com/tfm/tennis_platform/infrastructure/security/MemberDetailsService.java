package com.tfm.tennis_platform.infrastructure.security;

import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberDetailsService implements UserDetailsService {

    private final JpaMemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        MemberEntity user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User do not exist"));

        log.info("Email read from DB: {}", user.getEmail());//.charAt(0) + "****");

        return new User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getTier().toUpperCase()))
        );
    }

}
