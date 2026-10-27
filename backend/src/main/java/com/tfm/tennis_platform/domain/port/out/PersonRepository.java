package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Person;

import java.util.Optional;
import java.util.UUID;

public interface PersonRepository {

    Person save(Person person);

    Optional<Person> findById(UUID id);
}
