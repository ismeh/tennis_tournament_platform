package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.ProPlayerQueryService;
import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.infrastructure.controller.dto.ProPlayerSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProPlayerControllerTest {

    @Mock
    private ProPlayerQueryService proPlayerQueryService;
    @InjectMocks
    private ProPlayerController controller;

    @Test
    void should_search_pro_players() {
        ProPlayer player = ProPlayer.builder()
                .id(1)
                .license("L001")
                .fullName("Rafael Nadal")
                .firstName("Rafael")
                .lastName("Nadal")
                .rankingPosition(1)
                .ageCategory("30+")
                .clubName("RC Madrid")
                .birthDate(LocalDate.of(1986, 6, 3))
                .gender("M")
                .build();

        when(proPlayerQueryService.search("Nadal", "M", "30+")).thenReturn(List.of(player));

        ResponseEntity<List<ProPlayerSearchResponse>> result = controller.search("Nadal", "M", "30+");

        assertThat(result.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).id()).isEqualTo(1);
        assertThat(result.getBody().get(0).license()).isEqualTo("L001");
        assertThat(result.getBody().get(0).fullName()).isEqualTo("Rafael Nadal");
        assertThat(result.getBody().get(0).firstName()).isEqualTo("Rafael");
        assertThat(result.getBody().get(0).lastName()).isEqualTo("Nadal");
        assertThat(result.getBody().get(0).rankingPosition()).isEqualTo(1);
        assertThat(result.getBody().get(0).ageCategory()).isEqualTo("30+");
        assertThat(result.getBody().get(0).clubName()).isEqualTo("RC Madrid");
        assertThat(result.getBody().get(0).birthDate()).isEqualTo(LocalDate.of(1986, 6, 3));
        assertThat(result.getBody().get(0).gender()).isEqualTo("M");
    }

    @Test
    void should_return_empty_when_no_results() {
        when(proPlayerQueryService.search(null, null, null)).thenReturn(List.of());

        ResponseEntity<List<ProPlayerSearchResponse>> result = controller.search(null, null, null);

        assertThat(result.getBody()).isEmpty();
    }
}
