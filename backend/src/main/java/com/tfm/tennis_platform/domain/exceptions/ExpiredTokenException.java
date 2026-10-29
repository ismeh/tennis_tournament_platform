package com.tfm.tennis_platform.domain.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un token JWT ha expirado.
 * Corresponde al código HTTP 401 Unauthorized.
 */
public class ExpiredTokenException extends BaseException {

    public ExpiredTokenException(String message) {
        super(
            message,
            "EXPIRED_TOKEN",
            HttpStatus.UNAUTHORIZED.value()
        );
    }

    public ExpiredTokenException(String message, Throwable cause) {
        super(
            message,
            cause,
            "EXPIRED_TOKEN",
            HttpStatus.UNAUTHORIZED.value()
        );
    }
}
