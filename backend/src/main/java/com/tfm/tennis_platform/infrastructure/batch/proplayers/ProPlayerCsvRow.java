package com.tfm.tennis_platform.infrastructure.batch.proplayers;

import java.time.LocalDate;

record ProPlayerCsvRow(
    Integer points,
    String license,
    String name,
    Integer position,
    Integer territorialPosition,
    Integer provincialPosition,
    Integer clubPosition,
    Integer agePosition,
    String age,
    String clubName,
    String provincialName,
    String territorialName,
    String categoryName,
    Integer awardedPoints,
    LocalDate birthDate,
    String gender
) {
}
