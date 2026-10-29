package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.PersonQueryService;
import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.infrastructure.controller.dto.PersonSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonControllerTest {

    @Mock
    private PersonQueryService personQueryService;
    @InjectMocks
    private PersonController controller;

    @Test
    void should_search_persons() {
        UUID id = UUID.randomUUID();
        Person person = Person.builder()
                .id(id)
                .tennisId("T001")
                .firstName("Ana")
                .lastName("Garcia")
                .nationality("ESP")
                .birthDate(LocalDate.of(1990, 5, 15))
                .gender("F")
                .build();

        when(personQueryService.search("Ana")).thenReturn(List.of(person));

        ResponseEntity<List<PersonSearchResponse>> result = controller.search("Ana");

        assertThat(result.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).id()).isEqualTo(id);
        assertThat(result.getBody().get(0).tennisId()).isEqualTo("T001");
        assertThat(result.getBody().get(0).firstName()).isEqualTo("Ana");
        assertThat(result.getBody().get(0).lastName()).isEqualTo("Garcia");
        assertThat(result.getBody().get(0).nationality()).isEqualTo("ESP");
        assertThat(result.getBody().get(0).birthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(result.getBody().get(0).gender()).isEqualTo("F");
    }

    @Test
    void should_return_empty_when_no_results() {
        when(personQueryService.search(null)).thenReturn(List.of());

        ResponseEntity<List<PersonSearchResponse>> result = controller.search(null);

        assertThat(result.getBody()).isEmpty();
    }
}
