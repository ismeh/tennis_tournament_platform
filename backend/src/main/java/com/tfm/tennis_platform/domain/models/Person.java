package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Person {

    private final UUID id;
    private final String tennisId;
    private final String firstName;
    private final String lastName;
    private final String nationality;
    private final LocalDate birthDate;
    private final String gender;
}
