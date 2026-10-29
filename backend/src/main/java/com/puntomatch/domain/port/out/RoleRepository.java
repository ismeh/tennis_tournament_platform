package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.Role;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByName(String name);
}
