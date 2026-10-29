package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.commands.AgeCategoryOutput;
import com.tfm.tennis_platform.application.commands.CustomAgeCategoryRequest;
import com.tfm.tennis_platform.application.services.CustomAgeCategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAgeCategoryControllerTest {

    @Mock
    private CustomAgeCategoryService customAgeCategoryService;
    @InjectMocks
    private CustomAgeCategoryController controller;

    @Test
    void should_get_my_categories() {
        Principal principal = () -> "user@test.com";
        AgeCategoryOutput cat = new AgeCategoryOutput(1, "Custom30+");
        when(customAgeCategoryService.getMyCategories("user@test.com")).thenReturn(List.of(cat));

        List<AgeCategoryOutput> result = controller.getMyCategories(principal);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("Custom30+");
    }

    @Test
    void should_create_category() {
        Principal principal = () -> "user@test.com";
        CustomAgeCategoryRequest request = new CustomAgeCategoryRequest("NewCategory");
        AgeCategoryOutput created = new AgeCategoryOutput(2, "NewCategory");
        when(customAgeCategoryService.create(request, "user@test.com")).thenReturn(created);

        ResponseEntity<AgeCategoryOutput> result = controller.create(request, principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().category()).isEqualTo("NewCategory");
    }

    @Test
    void should_update_category() {
        Principal principal = () -> "user@test.com";
        CustomAgeCategoryRequest request = new CustomAgeCategoryRequest("Updated");
        AgeCategoryOutput updated = new AgeCategoryOutput(1, "Updated");
        when(customAgeCategoryService.update(1, request, "user@test.com")).thenReturn(updated);

        AgeCategoryOutput result = controller.update(1, request, principal);

        assertThat(result.category()).isEqualTo("Updated");
    }

    @Test
    void should_delete_category() {
        Principal principal = () -> "user@test.com";

        ResponseEntity<Void> result = controller.delete(1, principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(customAgeCategoryService).delete(1, "user@test.com");
    }
}
