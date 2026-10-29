package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.commands.AgeCategoryOutput;
import com.tfm.tennis_platform.application.commands.CustomAgeCategoryRequest;
import com.tfm.tennis_platform.domain.exceptions.DuplicateResourceException;
import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.AgeCategoryRef;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.AgeCategoryRefRepository;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAgeCategoryServiceTest {

    @Mock
    private AgeCategoryRefRepository ageCategoryRefRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomAgeCategoryService customAgeCategoryService;

    private static final String ORGANIZER_EMAIL = "organizer@example.com";
    private static final UUID ORGANIZER_ID = UUID.randomUUID();

    private Member buildOrganizer() {
        return Member.builder()
                .id(ORGANIZER_ID)
                .email(ORGANIZER_EMAIL)
                .role(UserRole.ORGANIZER)
                .build();
    }

    @Test
    void should_return_custom_categories_for_organizer() {
        when(memberRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(buildOrganizer()));
        when(ageCategoryRefRepository.findByOrganizerId(ORGANIZER_ID)).thenReturn(List.of(
                AgeCategoryRef.builder().id(100).category("Mi Categoria").organizerId(ORGANIZER_ID).build()
        ));

        List<AgeCategoryOutput> result = customAgeCategoryService.getMyCategories(ORGANIZER_EMAIL);

        assertEquals(1, result.size());
        assertEquals("Mi Categoria", result.get(0).category());
        assertTrue(result.get(0).custom());
    }

    @Test
    void should_create_custom_category() {
        when(memberRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(buildOrganizer()));
        when(ageCategoryRefRepository.existsByOrganizerIdAndCategory(ORGANIZER_ID, "Nueva")).thenReturn(false);
        when(ageCategoryRefRepository.save(any(AgeCategoryRef.class))).thenAnswer(inv -> {
            AgeCategoryRef cat = inv.getArgument(0);
            return AgeCategoryRef.builder().id(200).category(cat.getCategory()).organizerId(cat.getOrganizerId()).build();
        });

        AgeCategoryOutput result = customAgeCategoryService.create(
                new CustomAgeCategoryRequest("Nueva"), ORGANIZER_EMAIL);

        assertEquals(200, result.id());
        assertEquals("Nueva", result.category());
        assertTrue(result.custom());

        ArgumentCaptor<AgeCategoryRef> captor = ArgumentCaptor.forClass(AgeCategoryRef.class);
        verify(ageCategoryRefRepository).save(captor.capture());
        assertEquals("Nueva", captor.getValue().getCategory());
        assertEquals(ORGANIZER_ID, captor.getValue().getOrganizerId());
    }

    @Test
    void should_throw_when_name_is_blank() {
        when(memberRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(buildOrganizer()));

        assertThrows(InvalidArgumentException.class, () ->
                customAgeCategoryService.create(new CustomAgeCategoryRequest("  "), ORGANIZER_EMAIL));
        assertThrows(InvalidArgumentException.class, () ->
                customAgeCategoryService.create(new CustomAgeCategoryRequest(null), ORGANIZER_EMAIL));
    }

    @Test
    void should_throw_when_name_already_exists_for_organizer() {
        when(memberRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(buildOrganizer()));
        when(ageCategoryRefRepository.existsByOrganizerIdAndCategory(ORGANIZER_ID, "Duplicada")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                customAgeCategoryService.create(new CustomAgeCategoryRequest("Duplicada"), ORGANIZER_EMAIL));
    }

    @Test
    void should_update_custom_category() {
        Member organizer = buildOrganizer();
        when(memberRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(organizer));
        when(ageCategoryRefRepository.findById(100)).thenReturn(Optional.of(
                AgeCategoryRef.builder().id(100).category("Vieja").organizerId(ORGANIZER_ID).build()
        ));
        when(ageCategoryRefRepository.existsByOrganizerIdAndCategory(ORGANIZER_ID, "Nueva")).thenReturn(false);
        when(ageCategoryRefRepository.save(any(AgeCategoryRef.class))).thenAnswer(inv -> inv.getArgument(0));

        AgeCategoryOutput result = customAgeCategoryService.update(
                100, new CustomAgeCategoryRequest("Nueva"), ORGANIZER_EMAIL);

        assertEquals("Nueva", result.category());
        assertTrue(result.custom());
    }

    @Test
    void should_throw_when_updating_category_not_owned() {
        when(memberRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(buildOrganizer()));
        when(ageCategoryRefRepository.findById(100)).thenReturn(Optional.of(
                AgeCategoryRef.builder().id(100).category("Ajena").organizerId(UUID.randomUUID()).build()
        ));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                customAgeCategoryService.update(100, new CustomAgeCategoryRequest("X"), ORGANIZER_EMAIL));
    }

    @Test
    void should_delete_custom_category() {
        when(memberRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(buildOrganizer()));
        when(ageCategoryRefRepository.findById(100)).thenReturn(Optional.of(
                AgeCategoryRef.builder().id(100).category("ParaBorrar").organizerId(ORGANIZER_ID).build()
        ));

        assertDoesNotThrow(() ->
                customAgeCategoryService.delete(100, ORGANIZER_EMAIL));
        verify(ageCategoryRefRepository).deleteById(100);
    }

    @Test
    void should_throw_when_deleting_category_not_owned() {
        when(memberRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(buildOrganizer()));
        when(ageCategoryRefRepository.findById(100)).thenReturn(Optional.of(
                AgeCategoryRef.builder().id(100).category("Ajena").organizerId(UUID.randomUUID()).build()
        ));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                customAgeCategoryService.delete(100, ORGANIZER_EMAIL));
    }

    @Test
    void should_throw_when_member_not_found() {
        when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                customAgeCategoryService.getMyCategories("unknown@example.com"));
    }

    @Test
    void should_throw_when_member_is_not_organizer() {
        Member player = Member.builder()
                .id(ORGANIZER_ID)
                .email(ORGANIZER_EMAIL)
                .role(UserRole.PLAYER)
                .build();
        when(memberRepository.findByEmail(ORGANIZER_EMAIL)).thenReturn(Optional.of(player));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                customAgeCategoryService.getMyCategories(ORGANIZER_EMAIL));
    }
}
