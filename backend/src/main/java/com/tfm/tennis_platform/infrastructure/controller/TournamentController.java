package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentResponse;
import com.tfm.tennis_platform.infrastructure.controller.mapper.TournamentWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentWebMapper tournamentMapper;

    @PostMapping
    public ResponseEntity<TournamentResponse> create(@RequestBody TournamentRequest request, Principal principal) {
        Tournament tournament = tournamentMapper.toDomain(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tournamentMapper.toResponse(tournamentService.create(tournament, principal.getName())));
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getAll() {
        return ResponseEntity.ok(tournamentService.findAll().stream()
                .map(tournamentMapper::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getById(@PathVariable UUID id) {
        return tournamentService.findById(id)
                .map(t -> ResponseEntity.ok(tournamentMapper.toResponse(t)))
                .orElse(ResponseEntity.notFound().build());
    }
}
