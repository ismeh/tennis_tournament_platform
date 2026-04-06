package com.tfm.tennis_platform.domain.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un token JWT es inválido (malformado, firma incorrecta, etc.).
 * Corresponde al código HTTP 401 Unauthorized.
 */
public class InvalidTokenException extends BaseException {

    public InvalidTokenException(String message) {
        super(
            message,
            "INVALID_TOKEN",
            HttpStatus.UNAUTHORIZED.value()
        );
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(
            message,
            cause,
            "INVALID_TOKEN",
            HttpStatus.UNAUTHORIZED.value()
        );
    }
}
