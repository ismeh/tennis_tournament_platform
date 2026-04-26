package com.tfm.tennis_platform.domain.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando se intenta crear un recurso que ya existe.
 * Corresponde al código HTTP 409 Conflict.
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s con %s '%s' ya existe", resourceName, fieldName, fieldValue),
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
}
