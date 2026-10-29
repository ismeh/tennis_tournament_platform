package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.Person;

import java.util.Optional;
import java.util.UUID;

public interface PersonRepository {

    Person save(Person person);

    Optional<Person> findById(UUID id);
}
