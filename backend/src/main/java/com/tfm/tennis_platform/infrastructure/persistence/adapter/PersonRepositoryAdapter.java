package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.domain.port.out.PersonRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.PersonMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
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
