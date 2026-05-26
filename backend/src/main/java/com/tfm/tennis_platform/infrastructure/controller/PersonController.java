package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.PersonQueryService;
import com.tfm.tennis_platform.infrastructure.controller.dto.PersonSearchResponse;
import com.tfm.tennis_platform.domain.models.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonQueryService personQueryService;

    @GetMapping
    public ResponseEntity<List<PersonSearchResponse>> search(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(personQueryService.search(query).stream()
                .map(PersonController::toResponse)
                .toList());
    }

    private static PersonSearchResponse toResponse(Person person) {
        return new PersonSearchResponse(
                person.getId(),
                person.getTennisId(),
                person.getFirstName(),
                person.getLastName(),
                person.getNationality(),
                person.getBirthDate(),
                person.getGender()
        );
    }
}