package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.domain.port.out.ProPlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProPlayerQueryServiceTest {

    @Mock
    private ProPlayerRepository proPlayerRepository;

    @InjectMocks
    private ProPlayerQueryService proPlayerQueryService;

    @Test
    void searchShouldReturnTopPlayersWhenQueryIsBlank() {
        List<ProPlayer> expected = List.of(ProPlayer.builder().id(1).fullName("NADAL, RAFAEL").build());
        when(proPlayerRepository.findTop10()).thenReturn(expected);

        List<ProPlayer> actual = proPlayerQueryService.search(" ");

        assertEquals(expected, actual);
        verify(proPlayerRepository).findTop10();
    }

    @Test
    void searchShouldTrimQueryAndDelegateToRepository() {
        List<ProPlayer> expected = List.of(ProPlayer.builder().id(2).fullName("FEDERER, ROGER").build());
        when(proPlayerRepository.search("Roger", null, null)).thenReturn(expected);

        List<ProPlayer> actual = proPlayerQueryService.search("  Roger ");

        assertEquals(expected, actual);
        verify(proPlayerRepository).search("Roger", null, null);
    }

    @Test
    void searchShouldNormalizeFiltersAndDelegateToRepository() {
        List<ProPlayer> expected = List.of(ProPlayer.builder().id(3).fullName("SWIATEK, IGA").build());
        when(proPlayerRepository.search(null, "FEMALE", "ABSOLUTA")).thenReturn(expected);

        List<ProPlayer> actual = proPlayerQueryService.search(" ", " female ", " absoluta ");

        assertEquals(expected, actual);
        verify(proPlayerRepository).search(null, "FEMALE", "ABSOLUTA");
    }
}
