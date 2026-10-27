package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * A sub-competition within a tournament, identified by an age category and a gender.
 *
 * <p>Stored in the {@code events} table. The age category is a reference to
 * {@code ref_age_category}, which is why it is an {@link Integer} rather than a UUID.
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class Event {

    private final UUID id;

    private final UUID tournamentId;

    private final Integer categoryId;

    private final String gender;
}
