package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.domain.port.out.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonQueryServiceTest {

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private PersonQueryService personQueryService;

    @Test
    void search_returns_top10_when_query_is_null() {
        Person person = Person.builder().firstName("John").lastName("Doe").build();
        when(personRepository.findTop10()).thenReturn(List.of(person));

        List<Person> result = personQueryService.search(null);

        assertEquals(1, result.size());
        verify(personRepository).findTop10();
        verify(personRepository, never()).searchByQuery(any());
    }

    @Test
    void search_returns_top10_when_query_is_blank() {
        when(personRepository.findTop10()).thenReturn(List.of());

        List<Person> result = personQueryService.search("  ");

        assertTrue(result.isEmpty());
        verify(personRepository).findTop10();
        verify(personRepository, never()).searchByQuery(any());
    }

    @Test
    void search_delegates_to_searchByQuery_when_query_present() {
        Person person = Person.builder().firstName("Jane").lastName("Smith").build();
        when(personRepository.searchByQuery("Jane")).thenReturn(List.of(person));

        List<Person> result = personQueryService.search("Jane");

        assertEquals(1, result.size());
        assertEquals("Jane", result.get(0).getFirstName());
        verify(personRepository).searchByQuery("Jane");
        verify(personRepository, never()).findTop10();
    }

    @Test
    void search_trims_query() {
        when(personRepository.searchByQuery("Jane")).thenReturn(List.of());

        personQueryService.search("  Jane  ");

        verify(personRepository).searchByQuery("Jane");
    }
}
