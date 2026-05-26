package com.tfm.tennis_platform.domain.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un recurso solicitado no existe.
 * Corresponde al código HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(
            String.format("%s con ID '%s' no encontrado", resourceName, resourceId),
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
}
