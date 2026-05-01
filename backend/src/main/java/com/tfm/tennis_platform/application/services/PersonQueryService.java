package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.domain.port.out.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonQueryService {

    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public List<Person> search(String query) {
        if (query == null || query.isBlank()) {
            return personRepository.findTop20();
        }

        return personRepository.searchByQuery(query.trim());
    }
}