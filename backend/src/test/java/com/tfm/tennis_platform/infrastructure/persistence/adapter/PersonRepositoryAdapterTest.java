package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.PersonMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonRepositoryAdapterTest {

    @Mock
    private JpaPersonRepository jpaPersonRepository;
    @Mock
    private PersonMapper personMapper;
    @InjectMocks
    private PersonRepositoryAdapter adapter;

    @Test
    void should_save_person() {
        UUID id = UUID.randomUUID();
        Person domain = Person.builder().id(id).firstName("Ana").build();
        PersonEntity entity = PersonEntity.builder().id(id).firstName("Ana").build();
        PersonEntity savedEntity = PersonEntity.builder().id(id).firstName("Ana").build();
        Person mappedDomain = Person.builder().id(id).firstName("Ana").build();

        when(personMapper.toEntity(domain)).thenReturn(entity);
        when(jpaPersonRepository.saveAndFlush(entity)).thenReturn(savedEntity);
        when(personMapper.toDomain(savedEntity)).thenReturn(mappedDomain);

        Person result = adapter.save(domain);

        assertThat(result).isEqualTo(mappedDomain);
        verify(personMapper).toEntity(domain);
        verify(jpaPersonRepository).saveAndFlush(entity);
        verify(personMapper).toDomain(savedEntity);
    }

    @Test
    void should_find_by_id() {
        UUID id = UUID.randomUUID();
        PersonEntity entity = PersonEntity.builder().id(id).firstName("Ana").build();
        Person mapped = Person.builder().id(id).firstName("Ana").build();

        when(jpaPersonRepository.findById(id)).thenReturn(Optional.of(entity));
        when(personMapper.toDomain(entity)).thenReturn(mapped);

        Optional<Person> result = adapter.findById(id);

        assertThat(result).contains(mapped);
    }

    @Test
    void should_return_empty_when_not_found() {
        UUID id = UUID.randomUUID();
        when(jpaPersonRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Person> result = adapter.findById(id);

        assertThat(result).isEmpty();
    }

    @Test
    void should_find_top_10() {
        PersonEntity e1 = PersonEntity.builder().id(UUID.randomUUID()).firstName("Ana").build();
        PersonEntity e2 = PersonEntity.builder().id(UUID.randomUUID()).firstName("Luis").build();
        Person d1 = Person.builder().id(e1.getId()).firstName("Ana").build();
        Person d2 = Person.builder().id(e2.getId()).firstName("Luis").build();

        when(jpaPersonRepository.findTop10ByOrderByFirstNameAscLastNameAsc()).thenReturn(List.of(e1, e2));
        when(personMapper.toDomain(e1)).thenReturn(d1);
        when(personMapper.toDomain(e2)).thenReturn(d2);

        List<Person> result = adapter.findTop10();

        assertThat(result).hasSize(2).containsExactly(d1, d2);
    }

    @Test
    void should_search_by_query() {
        UUID id = UUID.randomUUID();
        PersonEntity entity = PersonEntity.builder().id(id).firstName("Ana").build();
        Person mapped = Person.builder().id(id).firstName("Ana").build();

        when(jpaPersonRepository.searchByQuery(eq("Ana"), any())).thenReturn(List.of(entity));
        when(personMapper.toDomain(entity)).thenReturn(mapped);

        List<Person> result = adapter.searchByQuery("Ana");

        assertThat(result).hasSize(1).containsExactly(mapped);
    }

    @Test
    void should_anonymize() {
        UUID id = UUID.randomUUID();
        when(jpaPersonRepository.anonymize(id, "ANON")).thenReturn(1);

        adapter.anonymize(id, "ANON");

        verify(jpaPersonRepository).anonymize(id, "ANON");
    }
}
