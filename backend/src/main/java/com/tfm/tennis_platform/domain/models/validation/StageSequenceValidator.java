package com.tfm.tennis_platform.domain.models.validation;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;

import java.util.List;
import java.util.Set;

public class StageSequenceValidator {

    private static final Set<String> VALID_TYPES = Set.of(
            "SINGLE_ELIMINATION", "ROUND_ROBIN", "DOUBLE_ELIMINATION", "CONSOLATION"
    );

    private static final java.util.Map<String, Set<String>> TRANSITION_MATRIX = java.util.Map.ofEntries(
            java.util.Map.entry("ROUND_ROBIN", Set.of("ROUND_ROBIN", "SINGLE_ELIMINATION", "DOUBLE_ELIMINATION")),
            java.util.Map.entry("SINGLE_ELIMINATION", Set.of("SINGLE_ELIMINATION", "CONSOLATION")),
            java.util.Map.entry("DOUBLE_ELIMINATION", Set.of("ROUND_ROBIN", "SINGLE_ELIMINATION")),
            java.util.Map.entry("CONSOLATION", Set.of("ROUND_ROBIN", "SINGLE_ELIMINATION"))
    );

    public static void validate(List<String> phases) {
        if (phases == null || phases.isEmpty()) {
            throw new InvalidArgumentException("La lista de fases no puede estar vacía.");
        }

        for (int i = 0; i < phases.size(); i++) {
            String phase = phases.get(i);
            if (!VALID_TYPES.contains(phase)) {
                throw new InvalidArgumentException(
                        "Tipo de fase inválido: '" + phase + "'. Tipos válidos: " + VALID_TYPES);
            }
        }

        validateR1(phases);
        validateR2AndR3(phases);
        validateTransitionMatrix(phases);
    }

    private static void validateR1(List<String> phases) {
        if ("CONSOLATION".equals(phases.get(0))) {
            throw new InvalidArgumentException(
                    "R1: La primera fase no puede ser de tipo CONSOLATION. "
                    + "Un cuadro de consolación requiere jugadores eliminados en una fase previa.");
        }
    }

    private static void validateR2AndR3(List<String> phases) {
        for (int i = 1; i < phases.size(); i++) {
            String current = phases.get(i);
            String previous = phases.get(i - 1);

            if ("CONSOLATION".equals(current) && !"SINGLE_ELIMINATION".equals(previous)) {
                throw new InvalidArgumentException(
                        "R2: Una fase CONSOLATION solo es válida si la fase anterior es SINGLE_ELIMINATION. "
                        + "Fase " + (i + 1) + " es CONSOLATION pero la fase " + i + " es " + previous + ".");
            }

            if ("DOUBLE_ELIMINATION".equals(current) && (i + 1 < phases.size()) && "CONSOLATION".equals(phases.get(i + 1))) {
                throw new InvalidArgumentException(
                        "R3: Si la fase actual es DOUBLE_ELIMINATION, la siguiente fase no puede ser CONSOLATION. "
                        + "La doble eliminación ya incluye su propio cuadro de perdedores.");
            }
        }
    }

    private static void validateTransitionMatrix(List<String> phases) {
        for (int i = 0; i < phases.size() - 1; i++) {
            String current = phases.get(i);
            String next = phases.get(i + 1);
            Set<String> allowed = TRANSITION_MATRIX.get(current);

            if (allowed == null || !allowed.contains(next)) {
                throw new InvalidArgumentException(
                        "Transición inválida: '" + current + "' -> '" + next + "'. "
                        + "Desde " + current + " solo se permite: " + allowed);
            }
        }
    }
}
