package com.puntomatch.infrastructure.persistence.adapter;

import com.puntomatch.domain.port.out.RoleRepository;
import com.puntomatch.domain.models.Role;
import com.puntomatch.infrastructure.persistence.mapper.RoleMapper;
import com.puntomatch.infrastructure.persistence.repository.JpaRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final JpaRoleRepository roleRepository;
    private final RoleMapper mapper;

    @Override
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name).map(mapper::toDomain);
    }
}
