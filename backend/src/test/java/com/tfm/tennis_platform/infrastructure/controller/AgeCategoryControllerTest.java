package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.AgeCategoryService;
import com.tfm.tennis_platform.application.commands.AgeCategoryOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgeCategoryControllerTest {

    @Mock
    private AgeCategoryService ageCategoryService;
    @InjectMocks
    private AgeCategoryController controller;

    @Test
    void should_get_all() {
        AgeCategoryOutput cat = new AgeCategoryOutput(1, "30+");
        when(ageCategoryService.getAll()).thenReturn(List.of(cat));

        List<AgeCategoryOutput> result = controller.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1);
        assertThat(result.get(0).category()).isEqualTo("30+");
    }

    @Test
    void should_get_all_for_user() {
        Principal principal = () -> "user@test.com";
        AgeCategoryOutput cat = new AgeCategoryOutput(1, "30+");
        when(ageCategoryService.getAllForUser("user@test.com")).thenReturn(List.of(cat));

        List<AgeCategoryOutput> result = controller.getAllForUser(principal);

        assertThat(result).hasSize(1);
    }

    @Test
    void should_handle_null_principal() {
        when(ageCategoryService.getAllForUser(null)).thenReturn(List.of());

        List<AgeCategoryOutput> result = controller.getAllForUser(null);

        assertThat(result).isEmpty();
    }
}
