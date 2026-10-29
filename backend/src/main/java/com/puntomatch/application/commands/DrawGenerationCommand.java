package com.puntomatch.application.commands;

import java.util.UUID;
import java.util.List;

public record DrawGenerationCommand(
    UUID eventId,
    List<UUID> stageIds
) {}
