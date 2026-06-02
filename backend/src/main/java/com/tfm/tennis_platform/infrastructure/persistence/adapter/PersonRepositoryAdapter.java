package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.domain.port.out.PersonRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.PersonMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PersonRepositoryAdapter implements PersonRepository {

    private static final int SEARCH_LIMIT = 10;

    private final JpaPersonRepository jpaPersonRepository;
    private final PersonMapper personMapper;

    @Override
    public Person save(Person person) {
        PersonEntity entity = personMapper.toEntity(person);
        PersonEntity savedEntity = jpaPersonRepository.saveAndFlush(entity);
        return personMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Person> findById(UUID id) {
        return jpaPersonRepository.findById(id).map(personMapper::toDomain);
    }

    @Override
    public List<Person> findTop10() {
        return jpaPersonRepository.findTop10ByOrderByFirstNameAscLastNameAsc()
                .stream()
                .map(personMapper::toDomain)
                .toList();
    }

    @Override
    public List<Person> searchByQuery(String query) {
        return jpaPersonRepository.searchByQuery(query, PageRequest.of(0, SEARCH_LIMIT))
                .stream()
                .map(personMapper::toDomain)
                .toList();
    }
}
