package com.tfm.tennis_platform.infrastructure.controller.dto;

public record SetScoreRequest(
    int setNumber,
    int firstPlayerGames,
    int secondPlayerGames,
    Integer firstPlayerTiebreak,
    Integer secondPlayerTiebreak
) {}
