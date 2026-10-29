package com.tfm.tennis_platform.domain.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando se pasan argumentos inválidos a un servicio o controlador.
 * Corresponde al código HTTP 400 Bad Request.
 */
public class InvalidArgumentException extends BaseException {

    public InvalidArgumentException(String message) {
        super(
            message,
            "INVALID_ARGUMENT",
            HttpStatus.BAD_REQUEST.value()
        );
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(
            message,
            cause,
            "INVALID_ARGUMENT",
            HttpStatus.BAD_REQUEST.value()
        );
    }
}
