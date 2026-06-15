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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomAgeCategoryService {

    private final AgeCategoryRefRepository ageCategoryRefRepository;
    private final MemberRepository memberRepository;

    public List<AgeCategoryOutput> getMyCategories(String organizerEmail) {
        Member organizer = resolveOrganizer(organizerEmail);
        return ageCategoryRefRepository.findByOrganizerId(organizer.getId()).stream()
                .map(cat -> new AgeCategoryOutput(cat.getId(), cat.getCategory(), true))
                .toList();
    }

    public AgeCategoryOutput create(CustomAgeCategoryRequest request, String organizerEmail) {
        Member organizer = resolveOrganizer(organizerEmail);
        String name = request.name() != null ? request.name().trim() : "";
        if (name.isEmpty()) {
            throw new InvalidArgumentException("El nombre de la categoría no puede estar vacío.");
        }

        if (ageCategoryRefRepository.existsByOrganizerIdAndCategory(organizer.getId(), name)) {
            throw new DuplicateResourceException("CustomAgeCategory", "name", name);
        }

        AgeCategoryRef category = AgeCategoryRef.builder()
                .category(name)
                .organizerId(organizer.getId())
                .build();

        AgeCategoryRef saved = ageCategoryRefRepository.save(category);
        return new AgeCategoryOutput(saved.getId(), saved.getCategory(), true);
    }

    public AgeCategoryOutput update(Integer id, CustomAgeCategoryRequest request, String organizerEmail) {
        Member organizer = resolveOrganizer(organizerEmail);
        AgeCategoryRef existing = ageCategoryRefRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomAgeCategory", id));

        if (existing.getOrganizerId() == null || !existing.getOrganizerId().equals(organizer.getId())) {
            throw new AccessDeniedException("No tienes permiso para modificar esta categoría.");
        }

        String newName = request.name() != null ? request.name().trim() : "";
        if (newName.isEmpty()) {
            throw new InvalidArgumentException("El nombre de la categoría no puede estar vacío.");
        }
        if (!existing.getCategory().equalsIgnoreCase(newName)
                && ageCategoryRefRepository.existsByOrganizerIdAndCategory(organizer.getId(), newName)) {
            throw new DuplicateResourceException("CustomAgeCategory", "name", newName);
        }

        AgeCategoryRef updated = AgeCategoryRef.builder()
                .id(existing.getId())
                .category(newName)
                .organizerId(organizer.getId())
                .build();

        AgeCategoryRef saved = ageCategoryRefRepository.save(updated);
        return new AgeCategoryOutput(saved.getId(), saved.getCategory(), true);
    }

    public void delete(Integer id, String organizerEmail) {
        Member organizer = resolveOrganizer(organizerEmail);
        AgeCategoryRef existing = ageCategoryRefRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomAgeCategory", id));

        if (existing.getOrganizerId() == null || !existing.getOrganizerId().equals(organizer.getId())) {
            throw new AccessDeniedException("No tienes permiso para eliminar esta categoría.");
        }

        ageCategoryRefRepository.deleteById(id);
    }

    private Member resolveOrganizer(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Member", email));
        if (member.getRole() != UserRole.ORGANIZER) {
            throw new AccessDeniedException("Solo los organizadores pueden gestionar categorías personalizadas.");
        }
        return member;
    }
}
