package com.tfm.tennis_platform.domain.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando el usuario no tiene credenciales válidas.
 * Corresponde al código HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(
            message,
            "UNAUTHORIZED",
            HttpStatus.UNAUTHORIZED.value()
        );
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(
            message,
            cause,
            "UNAUTHORIZED",
            HttpStatus.UNAUTHORIZED.value()
        );
    }
}
