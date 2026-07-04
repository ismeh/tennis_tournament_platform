package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.domain.port.out.ClubRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubQueryServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private ClubQueryService clubQueryService;

    @Test
    void search_returns_empty_when_query_is_null() {
        List<Club> result = clubQueryService.search(null);
        assertTrue(result.isEmpty());
        verifyNoInteractions(clubRepository);
    }

    @Test
    void search_returns_empty_when_query_is_short() {
        List<Club> result = clubQueryService.search(" a ");
        assertTrue(result.isEmpty());
        verifyNoInteractions(clubRepository);
    }

    @Test
    void search_delegates_to_repository_when_query_is_valid() {
        Club club = Club.builder().id(UUID.randomUUID()).name("Club de Tenis").build();
        when(clubRepository.findByNameContaining("Club")).thenReturn(List.of(club));

        List<Club> result = clubQueryService.search("  Club  ");

        assertEquals(1, result.size());
        assertEquals("Club de Tenis", result.get(0).getName());
        verify(clubRepository).findByNameContaining("Club");
    }

    @Test
    void findOrCreate_returns_null_when_name_is_null() {
        Club result = clubQueryService.findOrCreate(null);
        assertNull(result);
        verifyNoInteractions(clubRepository);
    }

    @Test
    void findOrCreate_returns_null_when_name_is_empty() {
        Club result = clubQueryService.findOrCreate("   ");
        assertNull(result);
        verifyNoInteractions(clubRepository);
    }

    @Test
    void findOrCreate_returns_existing_club_when_found() {
        Club existing = Club.builder().id(UUID.randomUUID()).name("Club Real").build();
        when(clubRepository.findByNameIgnoreCase("Club Real")).thenReturn(Optional.of(existing));

        Club result = clubQueryService.findOrCreate("  Club Real  ");

        assertEquals(existing, result);
        verify(clubRepository).findByNameIgnoreCase("Club Real");
        verify(clubRepository, never()).save(any());
    }

    @Test
    void findOrCreate_creates_new_club_when_not_found() {
        Club saved = Club.builder().id(UUID.randomUUID()).name("New Club").build();
        when(clubRepository.findByNameIgnoreCase("New Club")).thenReturn(Optional.empty());
        when(clubRepository.save(any(Club.class))).thenReturn(saved);

        Club result = clubQueryService.findOrCreate("  New Club  ");

        assertEquals(saved, result);
        verify(clubRepository).findByNameIgnoreCase("New Club");
        verify(clubRepository).save(argThat(club -> "New Club".equals(club.getName())));
    }
}
