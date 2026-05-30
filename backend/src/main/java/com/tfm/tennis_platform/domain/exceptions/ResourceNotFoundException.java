package com.tfm.tennis_platform.domain.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un recurso solicitado no existe.
 * Corresponde al código HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(
            buildMessage(resourceName),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND.value()
        );
    }

    public ResourceNotFoundException(String message) {
        super(
            message,
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND.value()
        );
    }

    private static String buildMessage(String resourceName) {
        return switch (resourceName) {
            case "Tournament" -> "No se encontró el torneo solicitado.";
            case "Match" -> "No se encontró el partido solicitado.";
            case "Court" -> "No se encontró la pista solicitada.";
            case "Next match" -> "No se encontró el siguiente partido del cuadro.";
            case "Member" -> "No se encontró la cuenta solicitada.";
            default -> "No se encontró el recurso solicitado.";
        };
    }
}
