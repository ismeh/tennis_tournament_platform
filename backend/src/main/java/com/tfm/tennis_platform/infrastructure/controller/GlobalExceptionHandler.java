package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.domain.exceptions.BaseException;
import com.tfm.tennis_platform.infrastructure.controller.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, WebRequest request) {
        log.warn("Domain error: {} - {}", ex.getCode(), ex.getMessage());

        return buildResponse(
            ex.getCode(),
            ex.getMessage(),
            HttpStatus.valueOf(ex.getHttpStatus()),
            request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        log.warn("Request validation failed");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), normalizeValidationMessage(error.getDefaultMessage()))
        );

        return buildResponse(
            "VALIDATION_ERROR",
            "Revisa los campos marcados antes de continuar.",
            HttpStatus.BAD_REQUEST,
            request,
            fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());

        return buildResponse(
            "INVALID_REQUEST_BODY",
            "No se pudo leer la información enviada. Revisa el formato de los datos.",
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing request parameter: {}", ex.getParameterName());

        return buildResponse(
            "MISSING_PARAMETER",
            "Falta un dato obligatorio en la solicitud.",
            HttpStatus.BAD_REQUEST,
            request,
            Map.of(ex.getParameterName(), "Este dato es obligatorio.")
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Invalid request parameter type: {}", ex.getName());

        return buildResponse(
            "INVALID_PARAMETER",
            "Uno de los datos enviados no tiene un formato válido.",
            HttpStatus.BAD_REQUEST,
            request,
            Map.of(ex.getName(), "Revisa el formato de este dato.")
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid request: {}", ex.getMessage());

        return buildResponse(
            "INVALID_REQUEST",
            normalizeValidationMessage(ex.getMessage()),
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, WebRequest request) {
        log.warn("Business conflict: {}", ex.getMessage());

        return buildResponse(
            "BUSINESS_RULE_CONFLICT",
            normalizeValidationMessage(ex.getMessage()),
            HttpStatus.CONFLICT,
            request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getClass().getSimpleName());

        return buildResponse(
            "AUTHENTICATION_FAILED",
            "Email o contraseña incorrectos.",
            HttpStatus.UNAUTHORIZED,
            request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        return buildResponse(
            "ACCESS_DENIED",
            "No tienes permisos para realizar esta acción.",
            HttpStatus.FORBIDDEN,
            request
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            WebRequest request) {
        log.warn("Endpoint not found: {}", ex.getRequestURL());

        return buildResponse(
            "RESOURCE_NOT_FOUND",
            "La ruta solicitada no existe.",
            HttpStatus.NOT_FOUND,
            request
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex,
            WebRequest request) {
        log.warn("Concurrent modification while saving entity: {}", ex.getPersistentClassName());

        return buildResponse(
            "CONCURRENT_MODIFICATION",
            "El torneo fue modificado por otro usuario. Recarga y vuelve a intentarlo.",
            HttpStatus.CONFLICT,
            request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

        return buildResponse(
            "DATA_CONFLICT",
            "No se pudo guardar la información porque entra en conflicto con datos existentes.",
            HttpStatus.CONFLICT,
            request
        );
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
        log.debug("Client disconnected from SSE stream: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request, HttpServletResponse response) {
        if (response.isCommitted()) {
            log.error("Unexpected error after response committed", ex);
            return null;
        }
        if (MediaType.TEXT_EVENT_STREAM_VALUE.equals(response.getContentType())) {
            log.error("Unexpected error on SSE stream, resetting content type", ex);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
        BaseException baseException = findCause(ex, BaseException.class);
        if (baseException != null) {
            log.warn("Wrapped domain error: {} - {}", baseException.getCode(), baseException.getMessage());
            return buildResponse(
                baseException.getCode(),
                baseException.getMessage(),
                HttpStatus.valueOf(baseException.getHttpStatus()),
                request
            );
        }

        IllegalArgumentException illegalArgumentException = findCause(ex, IllegalArgumentException.class);
        if (illegalArgumentException != null) {
            log.warn("Wrapped invalid request: {}", illegalArgumentException.getMessage());
            return buildResponse(
                "INVALID_REQUEST",
                normalizeValidationMessage(illegalArgumentException.getMessage()),
                HttpStatus.BAD_REQUEST,
                request
            );
        }

        IllegalStateException illegalStateException = findCause(ex, IllegalStateException.class);
        if (illegalStateException != null) {
            log.warn("Wrapped business conflict: {}", illegalStateException.getMessage());
            return buildResponse(
                "BUSINESS_RULE_CONFLICT",
                normalizeValidationMessage(illegalStateException.getMessage()),
                HttpStatus.CONFLICT,
                request
            );
        }

        log.error("Unexpected error", ex);

        return buildResponse(
            "INTERNAL_SERVER_ERROR",
            "No se pudo completar la operación. Inténtalo de nuevo más tarde.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(String code, String message, HttpStatus status, WebRequest request) {
        return buildResponse(code, message, status, request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(String code, String message, HttpStatus status, WebRequest request, Object details) {
        ErrorResponse errorResponse = new ErrorResponse(
            code,
            message,
            status.value(),
            LocalDateTime.now(),
            extractPath(request),
            details
        );
        return ResponseEntity
            .status(status)
            .body(errorResponse);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> targetType) {
        Throwable current = throwable;
        while (current != null) {
            if (targetType.isInstance(current)) {
                return targetType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private String normalizeValidationMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Revisa los datos enviados antes de continuar.";
        }

        return switch (message) {
            case "name must not be null", "name must not be empty" -> "El nombre del torneo es obligatorio.";
            case "playPeriod must not be null" -> "Las fechas de juego son obligatorias.";
            case "inscriptionPeriod must not be null" -> "Las fechas de inscripción son obligatorias.";
            case "surface must not be null" -> "La superficie del torneo es obligatoria.";
            case "maxPlayers must not be null" -> "El número máximo de jugadores es obligatorio.";
            case "maxPlayers must be greater than 0" -> "El número máximo de jugadores debe ser mayor que cero.";
            case "location must not be null" -> "La ubicación del torneo es obligatoria.";
            case "newEvents must not be null" -> "Debes enviar al menos un evento válido.";
            case "Duplicate event tuple (categoryId + gender)" -> "No puedes repetir la misma categoría y género en un torneo.";
            case "gender is null or empty" -> "El género del evento es obligatorio.";
            case "gender must be 'MALE', 'FEMALE' or 'MIXED'", "gender debe ser MALE, FEMALE o MIXED" -> "El género debe ser masculino, femenino o mixto.";
            case "categoryId is null or negative" -> "La categoría del evento es obligatoria.";
            case "stageNumber must be greater than 0" -> "El número de fase debe ser mayor que cero.";
            case "stageType must not be null" -> "El tipo de fase es obligatorio.";
            case "drawType must not be null" -> "El tipo de cuadro es obligatorio.";
            case "Tournament not found" -> "No se encontró el torneo solicitado.";
            case "Event not found in tournament" -> "No se encontró el evento dentro del torneo.";
            case "No inscriptions found for event" -> "Este evento todavía no tiene jugadores inscritos.";
            case "Tournament status must not be null" -> "Selecciona un estado válido para el torneo.";
            default -> message;
        };
    }
}
