package com.tfm.tennis_platform.domain.exceptions;

/**
 * Excepción base para todas las excepciones del dominio.
 * Facilita el manejo centralizado de errores y permite
 * capturar cualquier excepción de negocio de la aplicación.
 */
public abstract class BaseException extends RuntimeException {

    protected final String code;
    protected final int httpStatus;

    public BaseException(String message, String code, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BaseException(String message, Throwable cause, String code, int httpStatus) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
