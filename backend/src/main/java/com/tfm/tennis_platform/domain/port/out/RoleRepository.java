package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Role;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByName(String name);
}
