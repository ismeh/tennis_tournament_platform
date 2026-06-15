package com.tfm.tennis_platform.infrastructure.controller.refs;

import com.tfm.tennis_platform.application.commands.NationalityOutput;
import com.tfm.tennis_platform.application.services.NationalityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/refs/nationalities")
@RequiredArgsConstructor
public class NationalityController {
    private final NationalityService nationalityService;

    @GetMapping
    public List<NationalityOutput> getAll() {
        return nationalityService.getAll();
    }
}
