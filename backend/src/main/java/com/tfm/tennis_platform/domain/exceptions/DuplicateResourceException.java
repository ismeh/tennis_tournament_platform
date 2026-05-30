package com.tfm.tennis_platform.domain.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando se intenta crear un recurso que ya existe.
 * Corresponde al código HTTP 409 Conflict.
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(
            buildMessage(resourceName, fieldName),
            "DUPLICATE_RESOURCE",
            HttpStatus.CONFLICT.value()
        );
    }

    public DuplicateResourceException(String message) {
        super(
            message,
            "DUPLICATE_RESOURCE",
            HttpStatus.CONFLICT.value()
        );
    }

    private static String buildMessage(String resourceName, String fieldName) {
        if ("Member".equals(resourceName) && "email".equals(fieldName)) {
            return "Ya existe una cuenta registrada con ese email.";
        }
        return "Ya existe un recurso con esos datos.";
    }
}
