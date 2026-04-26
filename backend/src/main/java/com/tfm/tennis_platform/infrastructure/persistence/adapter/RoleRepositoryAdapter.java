package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.RoleRepository;
import com.tfm.tennis_platform.domain.models.Role;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.RoleMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaRoleRepository;
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
