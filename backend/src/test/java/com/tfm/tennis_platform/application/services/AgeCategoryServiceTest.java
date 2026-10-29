package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.commands.AgeCategoryOutput;
import com.tfm.tennis_platform.domain.models.AgeCategoryRef;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.AgeCategoryRefRepository;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
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
class AgeCategoryServiceTest {

    @Mock
    private AgeCategoryRefRepository ageCategoryRefRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AgeCategoryService ageCategoryService;

    @Test
    void getAll_returns_all_categories() {
        AgeCategoryRef cat1 = AgeCategoryRef.builder().id(1).category("Sub-12").build();
        AgeCategoryRef cat2 = AgeCategoryRef.builder().id(2).category("Sub-14").build();
        when(ageCategoryRefRepository.findAll()).thenReturn(List.of(cat1, cat2));

        List<AgeCategoryOutput> result = ageCategoryService.getAll();

        assertEquals(2, result.size());
        assertEquals("Sub-12", result.get(0).category());
        assertEquals("Sub-14", result.get(1).category());
        assertFalse(result.get(0).custom());
    }

    @Test
    void getAll_returns_empty_list_when_no_categories() {
        when(ageCategoryRefRepository.findAll()).thenReturn(List.of());

        List<AgeCategoryOutput> result = ageCategoryService.getAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllForUser_returns_custom_and_standard_when_email_present() {
        UUID memberId = UUID.randomUUID();
        Member member = Member.builder().id(memberId).email("org@example.com").role(UserRole.ORGANIZER).build();

        AgeCategoryRef custom = AgeCategoryRef.builder().id(10).category("Mi Categoria").organizerId(memberId).build();
        AgeCategoryRef standard = AgeCategoryRef.builder().id(20).category("Sub-16").build();

        when(memberRepository.findByEmail("org@example.com")).thenReturn(Optional.of(member));
        when(ageCategoryRefRepository.findByOrganizerId(memberId)).thenReturn(List.of(custom));
        when(ageCategoryRefRepository.findAll()).thenReturn(List.of(standard));

        List<AgeCategoryOutput> result = ageCategoryService.getAllForUser("org@example.com");

        assertEquals(2, result.size());
        assertTrue(result.get(0).custom());
        assertEquals("Mi Categoria", result.get(0).category());
        assertFalse(result.get(1).custom());
        assertEquals("Sub-16", result.get(1).category());
    }

    @Test
    void getAllForUser_returns_only_standard_when_email_null() {
        AgeCategoryRef standard = AgeCategoryRef.builder().id(1).category("Sub-12").build();
        when(ageCategoryRefRepository.findAll()).thenReturn(List.of(standard));

        List<AgeCategoryOutput> result = ageCategoryService.getAllForUser(null);

        assertEquals(1, result.size());
        assertFalse(result.get(0).custom());
    }

    @Test
    void getAllForUser_returns_only_standard_when_member_not_found() {
        when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(ageCategoryRefRepository.findAll()).thenReturn(List.of());

        List<AgeCategoryOutput> result = ageCategoryService.getAllForUser("unknown@example.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllForUser_returns_only_standard_when_organizer_has_no_custom() {
        UUID memberId = UUID.randomUUID();
        Member member = Member.builder().id(memberId).email("org@example.com").role(UserRole.ORGANIZER).build();
        AgeCategoryRef standard = AgeCategoryRef.builder().id(1).category("Sub-18").build();

        when(memberRepository.findByEmail("org@example.com")).thenReturn(Optional.of(member));
        when(ageCategoryRefRepository.findByOrganizerId(memberId)).thenReturn(List.of());
        when(ageCategoryRefRepository.findAll()).thenReturn(List.of(standard));

        List<AgeCategoryOutput> result = ageCategoryService.getAllForUser("org@example.com");

        assertEquals(1, result.size());
        assertEquals("Sub-18", result.get(0).category());
    }
}
