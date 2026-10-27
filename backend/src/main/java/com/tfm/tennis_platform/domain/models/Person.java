package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A physical individual. Members link to a person through {@code Member.personId}; the link is
 * created when a member completes their profile.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class Person {

    private final UUID id;

    /** ITF/IPIN identifier, reused to hold the federation licence. */
    private final String tennisId;

    private final String firstName;

    private final String lastName;

    /** ISO 3166-1 alpha-3 country code. */
    private final String nationality;

    private final LocalDate birthDate;

    /** {@code MALE}, {@code FEMALE} or {@code MIXED}. */
    private final String gender;
}
