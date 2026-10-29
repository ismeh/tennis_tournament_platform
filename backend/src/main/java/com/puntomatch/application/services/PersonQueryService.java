package com.puntomatch.application.services;

import com.puntomatch.domain.models.Person;
import com.puntomatch.domain.port.out.PersonRepository;
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
            return personRepository.findTop10();
        }

        return personRepository.searchByQuery(query.trim());
    }
}
