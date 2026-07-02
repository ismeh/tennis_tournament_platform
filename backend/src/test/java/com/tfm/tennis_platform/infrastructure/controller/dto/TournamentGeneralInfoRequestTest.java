package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TournamentGeneralInfoRequestTest {

    @Test
    void hasFormalNameReturnsFalseWhenNull() {
        var request = buildWithAllNulls();

        assertThat(request.hasFormalName()).isFalse();
    }

    @Test
    void hasFormalNameReturnsTrueForValidName() {
        var request = new TournamentGeneralInfoRequest(
                "Open de Tenis", null, null, null, null, null, null, null, null, null, null, null, null);

        assertThat(request.hasFormalName()).isTrue();
    }

    @Test
    void hasFormalNameReturnsFalseForEmptyString() {
        var request = new TournamentGeneralInfoRequest(
                "", null, null, null, null, null, null, null, null, null, null, null, null);

        assertThat(request.hasFormalName()).isFalse();
    }

    @Test
    void hasFormalNameReturnsFalseForBlankString() {
        var request = new TournamentGeneralInfoRequest(
                "   ", null, null, null, null, null, null, null, null, null, null, null, null);

        assertThat(request.hasFormalName()).isFalse();
    }

    @Test
    void hasPlayPeriodReturnsFalseWhenBothDatesNull() {
        var request = buildWithAllNulls();

        assertThat(request.hasPlayPeriod()).isFalse();
    }

    @Test
    void hasPlayPeriodReturnsFalseWhenOnlyStartDateProvided() {
        var request = new TournamentGeneralInfoRequest(
                null, LocalDate.of(2026, 7, 1), null, null, null, null, null, null, null, null, null, null, null);

        assertThat(request.hasPlayPeriod()).isFalse();
    }

    @Test
    void hasPlayPeriodReturnsFalseWhenOnlyEndDateProvided() {
        var request = new TournamentGeneralInfoRequest(
                null, null, LocalDate.of(2026, 7, 10), null, null, null, null, null, null, null, null, null, null);

        assertThat(request.hasPlayPeriod()).isFalse();
    }

    @Test
    void hasPlayPeriodReturnsTrueWhenBothDatesProvided() {
        var request = new TournamentGeneralInfoRequest(
                null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
                null, null, null, null, null, null, null, null, null, null);

        assertThat(request.hasPlayPeriod()).isTrue();
    }

    @Test
    void hasInscriptionPeriodReturnsFalseWhenBothDatesNull() {
        var request = buildWithAllNulls();

        assertThat(request.hasInscriptionPeriod()).isFalse();
    }

    @Test
    void hasInscriptionPeriodReturnsFalseWhenOnlyStartDateProvided() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null, LocalDate.of(2026, 6, 1), null, null, null, null, null, null, null, null);

        assertThat(request.hasInscriptionPeriod()).isFalse();
    }

    @Test
    void hasInscriptionPeriodReturnsFalseWhenOnlyEndDateProvided() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null, null, LocalDate.of(2026, 6, 30), null, null, null, null, null, null, null);

        assertThat(request.hasInscriptionPeriod()).isFalse();
    }

    @Test
    void hasInscriptionPeriodReturnsTrueWhenBothDatesProvided() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                null, null, null, null, null, null, null);

        assertThat(request.hasInscriptionPeriod()).isTrue();
    }

    @Test
    void hasSurfaceCategoryReturnsFalseWhenNull() {
        var request = buildWithAllNulls();

        assertThat(request.hasSurfaceCategory()).isFalse();
    }

    @Test
    void hasSurfaceCategoryReturnsTrueWhenProvided() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null, null, null, Surface.CLAY, null, null, null, null, null, null);

        assertThat(request.hasSurfaceCategory()).isTrue();
    }

    @Test
    void hasMaxPlayersReturnsFalseWhenNull() {
        var request = buildWithAllNulls();

        assertThat(request.hasMaxPlayers()).isFalse();
    }

    @Test
    void hasMaxPlayersReturnsFalseWhenZero() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null, null, null, null, 0, null, null, null, null, null);

        assertThat(request.hasMaxPlayers()).isFalse();
    }

    @Test
    void hasMaxPlayersReturnsFalseWhenNegative() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null, null, null, null, -1, null, null, null, null, null);

        assertThat(request.hasMaxPlayers()).isFalse();
    }

    @Test
    void hasMaxPlayersReturnsTrueWhenPositive() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null, null, null, null, 10, null, null, null, null, null);

        assertThat(request.hasMaxPlayers()).isTrue();
    }

    @Test
    void hasLocationReturnsFalseWhenNull() {
        var request = buildWithAllNulls();

        assertThat(request.hasLocation()).isFalse();
    }

    @Test
    void hasLocationReturnsTrueForValidLocation() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null, null, null, null, null,
                "Madrid, España", null, null, null, null);

        assertThat(request.hasLocation()).isTrue();
    }

    @Test
    void hasLocationReturnsFalseForEmptyString() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null, null, null, null, null,
                "", null, null, null, null);

        assertThat(request.hasLocation()).isFalse();
    }

    @Test
    void hasLocationReturnsFalseForBlankString() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, null, null, null, null, null,
                "   ", null, null, null, null);

        assertThat(request.hasLocation()).isFalse();
    }

    @Test
    void hasTournamentStartTimeReturnsFalseWhenNull() {
        var request = buildWithAllNulls();

        assertThat(request.hasTournamentStartTime()).isFalse();
    }

    @Test
    void hasTournamentStartTimeReturnsTrueWhenProvided() {
        var request = new TournamentGeneralInfoRequest(
                null, null, null, LocalTime.of(9, 0),
                null, null, null, null, null, null, null, null, null);

        assertThat(request.hasTournamentStartTime()).isTrue();
    }

    @Test
    void constructorWithAllArgsCreatesValidRecord() {
        var request = new TournamentGeneralInfoRequest(
                "Open de Tenis 2026",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                Surface.CLAY, 64,
                "Madrid, España", 40.4168, -3.7038,
                "ChIJ1xtwRFcZQg0R77Bn6XlXVwQ",
                "Madrid, Spain");

        assertThat(request.formalName()).isEqualTo("Open de Tenis 2026");
        assertThat(request.playStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(request.playEndDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(request.tournamentStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(request.inscriptionStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(request.inscriptionEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(request.surfaceCategory()).isEqualTo(Surface.CLAY);
        assertThat(request.maxPlayers()).isEqualTo(64);
        assertThat(request.location()).isEqualTo("Madrid, España");
        assertThat(request.locationLatitude()).isEqualTo(40.4168);
        assertThat(request.locationLongitude()).isEqualTo(-3.7038);
        assertThat(request.locationPlaceId()).isEqualTo("ChIJ1xtwRFcZQg0R77Bn6XlXVwQ");
        assertThat(request.locationFormattedAddress()).isEqualTo("Madrid, Spain");
    }

    @Test
    void constructorWithAllNullsCreatesValidRecord() {
        var request = buildWithAllNulls();

        assertThat(request.formalName()).isNull();
        assertThat(request.playStartDate()).isNull();
        assertThat(request.playEndDate()).isNull();
        assertThat(request.tournamentStartTime()).isNull();
        assertThat(request.inscriptionStartDate()).isNull();
        assertThat(request.inscriptionEndDate()).isNull();
        assertThat(request.surfaceCategory()).isNull();
        assertThat(request.maxPlayers()).isNull();
        assertThat(request.location()).isNull();
        assertThat(request.locationLatitude()).isNull();
        assertThat(request.locationLongitude()).isNull();
        assertThat(request.locationPlaceId()).isNull();
        assertThat(request.locationFormattedAddress()).isNull();
    }

    @Test
    void equalsReturnsTrueForSameValues() {
        var request1 = new TournamentGeneralInfoRequest(
                "Open de Tenis", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
                LocalTime.of(9, 0), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                Surface.CLAY, 64, "Madrid", 40.4168, -3.7038, "placeId", "address");

        var request2 = new TournamentGeneralInfoRequest(
                "Open de Tenis", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
                LocalTime.of(9, 0), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                Surface.CLAY, 64, "Madrid", 40.4168, -3.7038, "placeId", "address");

        assertThat(request1).isEqualTo(request2);
    }

    @Test
    void equalsReturnsFalseForDifferentValues() {
        var request1 = new TournamentGeneralInfoRequest(
                "Open de Tenis", null, null, null, null, null, null, null, null, null, null, null, null);

        var request2 = new TournamentGeneralInfoRequest(
                "Otro Torneo", null, null, null, null, null, null, null, null, null, null, null, null);

        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    void equalsReturnsFalseForNull() {
        var request = buildWithAllNulls();

        assertThat(request).isNotEqualTo(null);
    }

    @Test
    void equalsReturnsFalseForDifferentType() {
        var request = buildWithAllNulls();

        assertThat(request).isNotEqualTo("a string");
    }

    @Test
    void hashCodeIsConsistentForEqualRecords() {
        var request1 = new TournamentGeneralInfoRequest(
                "Open de Tenis", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
                null, null, null, Surface.HARD, 32, null, null, null, null, null);

        var request2 = new TournamentGeneralInfoRequest(
                "Open de Tenis", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
                null, null, null, Surface.HARD, 32, null, null, null, null, null);

        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    void toStringContainsAllFieldValues() {
        var request = new TournamentGeneralInfoRequest(
                "Open de Tenis", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
                null, null, null, Surface.CLAY, 64, null, null, null, null, null);

        var result = request.toString();

        assertThat(result).contains("Open de Tenis");
        assertThat(result).contains("2026-07-01");
        assertThat(result).contains("2026-07-10");
        assertThat(result).contains("CLAY");
        assertThat(result).contains("64");
    }

    @Test
    void componentsReturnsAllValuesInCorrectOrder() {
        var request = new TournamentGeneralInfoRequest(
                "Open de Tenis", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10),
                LocalTime.of(9, 0), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                Surface.CLAY, 64, "Madrid", 40.4168, -3.7038, "placeId", "address");

        RecordComponent[] components = request.getClass().getRecordComponents();

        assertThat(components).hasSize(15);
        assertThat(components[0].getName()).isEqualTo("formalName");
        assertThat(components[1].getName()).isEqualTo("playStartDate");
        assertThat(components[2].getName()).isEqualTo("playEndDate");
        assertThat(components[3].getName()).isEqualTo("tournamentStartTime");
        assertThat(components[4].getName()).isEqualTo("inscriptionStartDate");
        assertThat(components[5].getName()).isEqualTo("inscriptionEndDate");
        assertThat(components[6].getName()).isEqualTo("surfaceCategory");
        assertThat(components[7].getName()).isEqualTo("maxPlayers");
        assertThat(components[8].getName()).isEqualTo("location");
        assertThat(components[9].getName()).isEqualTo("locationLatitude");
        assertThat(components[10].getName()).isEqualTo("locationLongitude");
        assertThat(components[11].getName()).isEqualTo("locationPlaceId");
        assertThat(components[12].getName()).isEqualTo("locationFormattedAddress");
        assertThat(components[13].getName()).isEqualTo("setsPerMatch");
        assertThat(components[14].getName()).isEqualTo("decisiveTiebreakPoints");
    }

    @Test
    void recordComponentsHaveCorrectTypes() {
        var request = buildWithAllNulls();

        var components = request.getClass().getRecordComponents();

        assertThat(components[0].getType()).isEqualTo(String.class);
        assertThat(components[1].getType()).isEqualTo(LocalDate.class);
        assertThat(components[2].getType()).isEqualTo(LocalDate.class);
        assertThat(components[3].getType()).isEqualTo(LocalTime.class);
        assertThat(components[4].getType()).isEqualTo(LocalDate.class);
        assertThat(components[5].getType()).isEqualTo(LocalDate.class);
        assertThat(components[6].getType()).isEqualTo(Surface.class);
        assertThat(components[7].getType()).isEqualTo(Integer.class);
        assertThat(components[8].getType()).isEqualTo(String.class);
        assertThat(components[9].getType()).isEqualTo(Double.class);
        assertThat(components[10].getType()).isEqualTo(Double.class);
        assertThat(components[11].getType()).isEqualTo(String.class);
        assertThat(components[12].getType()).isEqualTo(String.class);
        assertThat(components[13].getType()).isEqualTo(Integer.class);
        assertThat(components[14].getType()).isEqualTo(Integer.class);
    }

    private TournamentGeneralInfoRequest buildWithAllNulls() {
        return new TournamentGeneralInfoRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
