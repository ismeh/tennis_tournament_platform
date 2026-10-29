package com.tfm.tennis_platform.infrastructure.controller.dto;

public record SetScoreResponse(
    int setNumber,
    int firstPlayerGames,
    int secondPlayerGames,
    Integer firstPlayerTiebreak,
    Integer secondPlayerTiebreak
) {}
