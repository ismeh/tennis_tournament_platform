package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.domain.exceptions.BaseException;
import com.tfm.tennis_platform.infrastructure.controller.dto.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para toda la aplicación.
 * Convierte excepciones en respuestas HTTP estandarizadas.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones custom del dominio (ResourceNotFoundException, InvalidArgumentException, etc.)
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, WebRequest request) {
        log.warn("Error de dominio: {} - {}", ex.getCode(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            ex.getCode(),
            ex.getMessage(),
            ex.getHttpStatus(),
            LocalDateTime.now(),
            extractPath(request)
        );

        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(errorResponse);
    }

    /**
     * Maneja excepciones de validación de argumentos (ej: @Valid fallido)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        log.warn("Error de validación de argumentos");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Datos de entrada inválidos",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            extractPath(request),
            fieldErrors
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Maneja excepciones de recurso no encontrado (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            WebRequest request) {
        log.warn("Recurso no encontrado: {}", ex.getRequestURL());

        ErrorResponse errorResponse = new ErrorResponse(
            "RESOURCE_NOT_FOUND",
            "El endpoint solicitado no existe",
            HttpStatus.NOT_FOUND.value(),
            LocalDateTime.now(),
            extractPath(request)
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex,
            WebRequest request) {
        log.warn("Conflicto de concurrencia al modificar entidad: {}", ex.getPersistentClassName());

        ErrorResponse errorResponse = new ErrorResponse(
            "CONCURRENT_MODIFICATION",
            "El torneo fue modificado por otro usuario. Recarga y vuelve a intentarlo.",
            HttpStatus.CONFLICT.value(),
            LocalDateTime.now(),
            extractPath(request)
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(errorResponse);
    }

    /**
     * Maneja excepciones genéricas no previstas.
     * Útil como último recurso para excepciones inesperadas.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Error inesperado", ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "Ha ocurrido un error interno en el servidor",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            LocalDateTime.now(),
            extractPath(request)
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    /**
     * Extrae el path de la solicitud desde WebRequest.
     */
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
