package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.infrastructure.realtime.TournamentSseBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentUpdatesController {

    private final TournamentSseBroadcaster broadcaster;

    @GetMapping(path = "/{tournamentId}/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable UUID tournamentId) {
        return broadcaster.subscribe(tournamentId);
    }
}
