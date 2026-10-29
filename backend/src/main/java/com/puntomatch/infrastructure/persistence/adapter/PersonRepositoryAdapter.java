package com.puntomatch.infrastructure.persistence.adapter;

import com.puntomatch.domain.models.Person;
import com.puntomatch.domain.port.out.PersonRepository;
import com.puntomatch.infrastructure.persistence.mapper.PersonMapper;
import com.puntomatch.infrastructure.persistence.repository.JpaPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PersonRepositoryAdapter implements PersonRepository {

    private final JpaPersonRepository personRepository;
    private final PersonMapper mapper;

    @Override
    public Person save(Person person) {
        return mapper.toDomain(personRepository.save(mapper.toEntity(person)));
    }

    @Override
    public Optional<Person> findById(UUID id) {
        return personRepository.findById(id).map(mapper::toDomain);
    }
}
