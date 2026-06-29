package com.tfm.tennis_platform.domain.models.validation;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("StageSequenceValidator")
class StageSequenceValidatorTest {

    @Test
    @DisplayName("valid single phase")
    void validSinglePhase() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("SINGLE_ELIMINATION")));
    }

    @Test
    @DisplayName("null phases throws")
    void nullPhases() {
        assertThrows(InvalidArgumentException.class, () -> StageSequenceValidator.validate(null));
    }

    @Test
    @DisplayName("empty phases throws")
    void emptyPhases() {
        assertThrows(InvalidArgumentException.class, () -> StageSequenceValidator.validate(List.of()));
    }

    @Test
    @DisplayName("invalid type throws")
    void invalidType() {
        assertThrows(InvalidArgumentException.class, () -> StageSequenceValidator.validate(List.of("INVALID")));
    }

    @Test
    @DisplayName("R1: CONSOLATION as first phase throws")
    void r1ConsolationFirst() {
        assertThrows(InvalidArgumentException.class, () -> StageSequenceValidator.validate(List.of("CONSOLATION")));
    }

    @Test
    @DisplayName("R2: CONSOLATION after non-SINGLE_ELIMINATION throws")
    void r2ConsolationAfterRoundRobin() {
        assertThrows(InvalidArgumentException.class,
                () -> StageSequenceValidator.validate(List.of("ROUND_ROBIN", "CONSOLATION")));
    }

    @Test
    @DisplayName("R2: CONSOLATION after SINGLE_ELIMINATION is valid")
    void r2ConsolationAfterSingleElimination() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("SINGLE_ELIMINATION", "CONSOLATION")));
    }

    @Test
    @DisplayName("R3: DOUBLE_ELIMINATION followed by CONSOLATION throws")
    void r3DoubleEliminationThenConsolation() {
        assertThrows(InvalidArgumentException.class,
                () -> StageSequenceValidator.validate(List.of("DOUBLE_ELIMINATION", "CONSOLATION")));
    }

    @Test
    @DisplayName("R3: DOUBLE_ELIMINATION followed by SINGLE_ELIMINATION is valid")
    void r3DoubleEliminationThenSingle() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("DOUBLE_ELIMINATION", "SINGLE_ELIMINATION")));
    }

    @Test
    @DisplayName("valid ROUND_ROBIN -> ROUND_ROBIN")
    void validRoundRobinChain() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("ROUND_ROBIN", "ROUND_ROBIN")));
    }

    @Test
    @DisplayName("valid ROUND_ROBIN -> SINGLE_ELIMINATION")
    void validRoundRobinToSingle() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("ROUND_ROBIN", "SINGLE_ELIMINATION")));
    }

    @Test
    @DisplayName("valid ROUND_ROBIN -> DOUBLE_ELIMINATION")
    void validRoundRobinToDouble() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("ROUND_ROBIN", "DOUBLE_ELIMINATION")));
    }

    @Test
    @DisplayName("valid SINGLE_ELIMINATION -> SINGLE_ELIMINATION")
    void validSingleToSingle() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("SINGLE_ELIMINATION", "SINGLE_ELIMINATION")));
    }

    @Test
    @DisplayName("invalid SINGLE_ELIMINATION -> ROUND_ROBIN transition throws")
    void invalidSingleToRoundRobin() {
        assertThrows(InvalidArgumentException.class,
                () -> StageSequenceValidator.validate(List.of("SINGLE_ELIMINATION", "ROUND_ROBIN")));
    }

    @Test
    @DisplayName("valid DOUBLE_ELIMINATION -> ROUND_ROBIN")
    void validDoubleToRoundRobin() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("DOUBLE_ELIMINATION", "ROUND_ROBIN")));
    }

    @Test
    @DisplayName("valid CONSOLATION -> ROUND_ROBIN")
    void validConsolationToRoundRobin() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("SINGLE_ELIMINATION", "CONSOLATION", "ROUND_ROBIN")));
    }

    @Test
    @DisplayName("valid CONSOLATION -> SINGLE_ELIMINATION")
    void validConsolationToSingle() {
        assertDoesNotThrow(() -> StageSequenceValidator.validate(List.of("SINGLE_ELIMINATION", "CONSOLATION", "SINGLE_ELIMINATION")));
    }

    @Test
    @DisplayName("invalid CONSOLATION -> ROUND_ROBIN as direct transition throws")
    void invalidConsolationToRoundRobinDirect() {
        assertThrows(InvalidArgumentException.class,
                () -> StageSequenceValidator.validate(List.of("CONSOLATION", "ROUND_ROBIN")));
    }
}
