package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.commands.AgeCategoryOutput;
import com.tfm.tennis_platform.domain.models.AgeCategoryRef;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.port.out.AgeCategoryRefRepository;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgeCategoryService {
    private final AgeCategoryRefRepository ageCategoryRefRepository;
    private final MemberRepository memberRepository;

    public List<AgeCategoryOutput> getAll() {
        return ageCategoryRefRepository.findAll().stream()
                .map(category -> new AgeCategoryOutput(category.getId(), category.getCategory()))
                .toList();
    }

    public List<AgeCategoryOutput> getAllForUser(String userEmail) {
        List<AgeCategoryOutput> result = new ArrayList<>();

        if (userEmail != null) {
            memberRepository.findByEmail(userEmail).ifPresent(member -> {
                List<AgeCategoryRef> custom = ageCategoryRefRepository.findByOrganizerId(member.getId());
                custom.forEach(cat -> result.add(new AgeCategoryOutput(cat.getId(), cat.getCategory(), true)));
            });
        }

        ageCategoryRefRepository.findAll().stream()
                .map(category -> new AgeCategoryOutput(category.getId(), category.getCategory()))
                .forEach(result::add);

        return result;
    }
}

