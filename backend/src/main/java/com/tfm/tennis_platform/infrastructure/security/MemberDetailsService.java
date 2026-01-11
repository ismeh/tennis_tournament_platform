package com.tfm.tennis_platform.infrastructure.security;

import com.tfm.tennis_platform.infrastructure.persistence.repository.MemberRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        MemberEntity user = memberRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("No existe"));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE" + user.getTier().toUpperCase()))
        );
    }

}
