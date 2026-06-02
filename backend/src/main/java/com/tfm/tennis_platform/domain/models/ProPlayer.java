package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder(toBuilder = true)
public class ProPlayer {

    private final Integer id;
    private final String license;
    private final String fullName;
    private final String firstName;
    private final String lastName;
    private final Integer rankingPosition;
    private final String ageCategory;
    private final String clubName;
    private final LocalDate birthDate;
    private final String gender;
}
