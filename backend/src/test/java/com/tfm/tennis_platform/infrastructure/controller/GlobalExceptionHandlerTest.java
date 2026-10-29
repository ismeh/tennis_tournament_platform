package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.domain.exceptions.BaseException;
import com.tfm.tennis_platform.domain.exceptions.DuplicateResourceException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.infrastructure.controller.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    private WebRequest request;

    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(WebRequest.class);
        response = mock(HttpServletResponse.class);
        lenient().when(request.getDescription(anyBoolean())).thenReturn("uri=/test-path");
    }

    private void stubRequestPath(String path) {
        when(request.getDescription(anyBoolean())).thenReturn("uri=" + path);
    }

    // ── handleBaseException ────────────────────────────────────────────

    @Test
    void handleBaseException_returnsCodeMessageAndHttpStatus() {
        stubRequestPath("/test-path");
        BaseException ex = new ResourceNotFoundException("Tournament not found");

        ResponseEntity<ErrorResponse> result = handler.handleBaseException(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(result.getBody().message()).isEqualTo("Tournament not found");
        assertThat(result.getBody().status()).isEqualTo(404);
        assertThat(result.getBody().path()).isEqualTo("/test-path");
        assertThat(result.getBody().timestamp()).isNotNull();
    }

    @Test
    void handleBaseException_withConflictStatus() {
        stubRequestPath("/test-path");
        BaseException ex = new DuplicateResourceException("email duplicado");

        ResponseEntity<ErrorResponse> result = handler.handleBaseException(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(result.getBody().status()).isEqualTo(409);
    }

    // ── handleMethodArgumentNotValid ───────────────────────────────────

    private MethodArgumentNotValidException createValidationException(
            org.springframework.validation.FieldError... fieldErrors) {
        org.springframework.validation.BindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "dto");
        for (org.springframework.validation.FieldError fe : fieldErrors) {
            bindingResult.addError(fe);
        }
        return new MethodArgumentNotValidException(null, bindingResult);
    }

    @Test
    void handleMethodArgumentNotValid_returnsFieldErrors() {
        org.springframework.validation.FieldError fieldError1 =
                new org.springframework.validation.FieldError("dto", "name", "name must not be empty");
        org.springframework.validation.FieldError fieldError2 =
                new org.springframework.validation.FieldError("dto", "surface", "surface must not be null");

        MethodArgumentNotValidException ex = createValidationException(fieldError1, fieldError2);

        ResponseEntity<ErrorResponse> result = handler.handleMethodArgumentNotValid(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(result.getBody().message()).isEqualTo("Revisa los campos marcados antes de continuar.");

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> details = (java.util.Map<String, String>) result.getBody().details();
        assertThat(details).containsEntry("name", "El nombre del torneo es obligatorio.");
        assertThat(details).containsEntry("surface", "La superficie del torneo es obligatoria.");
    }

    @Test
    void handleMethodArgumentNotValid_withUnmappedMessageReturnsOriginal() {
        org.springframework.validation.FieldError fieldError =
                new org.springframework.validation.FieldError("dto", "customField", "some custom validation");

        MethodArgumentNotValidException ex = createValidationException(fieldError);

        ResponseEntity<ErrorResponse> result = handler.handleMethodArgumentNotValid(ex, request);

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> details = (java.util.Map<String, String>) result.getBody().details();
        assertThat(details).containsEntry("customField", "some custom validation");
    }

    @Test
    void handleMethodArgumentNotValid_withEmptyFieldErrors() {
        MethodArgumentNotValidException ex = createValidationException();

        ResponseEntity<ErrorResponse> result = handler.handleMethodArgumentNotValid(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().details()).isNotNull();
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> details = (java.util.Map<String, String>) result.getBody().details();
        assertThat(details).isEmpty();
    }

    // ── handleMessageNotReadable ───────────────────────────────────────

    @Test
    void handleMessageNotReadable_returnsInvalidRequestBody() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("JSON parse error");

        ResponseEntity<ErrorResponse> result = handler.handleMessageNotReadable(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().code()).isEqualTo("INVALID_REQUEST_BODY");
        assertThat(result.getBody().message()).isEqualTo("No se pudo leer la información enviada. Revisa el formato de los datos.");
    }

    // ── handleMissingParameter ─────────────────────────────────────────

    @Test
    void handleMissingParameter_returnsMissingParameterWithFieldName() {
        stubRequestPath("/test-path");
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("tournamentId", "UUID");

        ResponseEntity<ErrorResponse> result = handler.handleMissingParameter(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().code()).isEqualTo("MISSING_PARAMETER");
        assertThat(result.getBody().message()).isEqualTo("Falta un dato obligatorio en la solicitud.");

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> details = (java.util.Map<String, String>) result.getBody().details();
        assertThat(details).containsEntry("tournamentId", "Este dato es obligatorio.");
    }

    // ── handleTypeMismatch ─────────────────────────────────────────────

    @Test
    void handleTypeMismatch_returnsInvalidParameterWithFieldName() {
        MethodArgumentTypeMismatchException ex =
                mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("eventId");

        ResponseEntity<ErrorResponse> result = handler.handleTypeMismatch(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().code()).isEqualTo("INVALID_PARAMETER");
        assertThat(result.getBody().message()).isEqualTo("Uno de los datos enviados no tiene un formato válido.");

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> details = (java.util.Map<String, String>) result.getBody().details();
        assertThat(details).containsEntry("eventId", "Revisa el formato de este dato.");
    }

    // ── handleIllegalArgument ──────────────────────────────────────────

    @Test
    void handleIllegalArgument_returnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Tournament not found");

        ResponseEntity<ErrorResponse> result = handler.handleIllegalArgument(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(result.getBody().message()).isEqualTo("No se encontró el torneo solicitado.");
    }

    @Test
    void handleIllegalArgument_withUnmappedMessageReturnsOriginal() {
        IllegalArgumentException ex = new IllegalArgumentException("Some random error");

        ResponseEntity<ErrorResponse> result = handler.handleIllegalArgument(ex, request);

        assertThat(result.getBody().message()).isEqualTo("Some random error");
    }

    @Test
    void handleIllegalArgument_withNullMessage() {
        IllegalArgumentException ex = new IllegalArgumentException((String) null);

        ResponseEntity<ErrorResponse> result = handler.handleIllegalArgument(ex, request);

        assertThat(result.getBody().message()).isEqualTo("Revisa los datos enviados antes de continuar.");
    }

    @Test
    void handleIllegalArgument_withBlankMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("   ");

        ResponseEntity<ErrorResponse> result = handler.handleIllegalArgument(ex, request);

        assertThat(result.getBody().message()).isEqualTo("Revisa los datos enviados antes de continuar.");
    }

    // ── handleIllegalState ─────────────────────────────────────────────

    @Test
    void handleIllegalState_returnsConflict() {
        IllegalStateException ex = new IllegalStateException("Tournament status must not be null");

        ResponseEntity<ErrorResponse> result = handler.handleIllegalState(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().code()).isEqualTo("BUSINESS_RULE_CONFLICT");
        assertThat(result.getBody().message()).isEqualTo("Selecciona un estado válido para el torneo.");
    }

    @Test
    void handleIllegalState_withUnmappedMessageReturnsOriginal() {
        IllegalStateException ex = new IllegalStateException("Business rule violated");

        ResponseEntity<ErrorResponse> result = handler.handleIllegalState(ex, request);

        assertThat(result.getBody().message()).isEqualTo("Business rule violated");
    }

    // ── handleAuthentication ───────────────────────────────────────────

    @Test
    void handleAuthentication_returnsUnauthorized() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> result = handler.handleAuthentication(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody().code()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(result.getBody().message()).isEqualTo("Email o contraseña incorrectos.");
    }

    // ── handleAccessDenied ─────────────────────────────────────────────

    @Test
    void handleAccessDenied_returnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<ErrorResponse> result = handler.handleAccessDenied(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody().code()).isEqualTo("ACCESS_DENIED");
        assertThat(result.getBody().message()).isEqualTo("No tienes permisos para realizar esta acción.");
    }

    // ── handleNoHandlerFound ───────────────────────────────────────────

    @Test
    void handleNoHandlerFound_returnsNotFound() {
        org.springframework.web.servlet.NoHandlerFoundException ex =
                mock(org.springframework.web.servlet.NoHandlerFoundException.class);
        when(ex.getRequestURL()).thenReturn("/api/nonexistent");

        ResponseEntity<ErrorResponse> result = handler.handleNoHandlerFound(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(result.getBody().message()).isEqualTo("La ruta solicitada no existe.");
    }

    // ── handleOptimisticLockingFailure ─────────────────────────────────

    @Test
    void handleOptimisticLockingFailure_returnsConflict() {
        ObjectOptimisticLockingFailureException ex =
                mock(ObjectOptimisticLockingFailureException.class);
        when(ex.getPersistentClassName()).thenReturn("Tournament");

        ResponseEntity<ErrorResponse> result = handler.handleOptimisticLockingFailure(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(result.getBody().message()).isEqualTo(
                "El torneo fue modificado por otro usuario. Recarga y vuelve a intentarlo.");
    }

    // ── handleDataIntegrityViolation ───────────────────────────────────

    @Test
    void handleDataIntegrityViolation_returnsConflict() {
        DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
        RuntimeException cause = new RuntimeException("unique constraint violation");
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorResponse> result = handler.handleDataIntegrityViolation(ex, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().code()).isEqualTo("DATA_CONFLICT");
        assertThat(result.getBody().message()).isEqualTo(
                "No se pudo guardar la información porque entra en conflicto con datos existentes.");
    }

    // ── handleAsyncRequestNotUsable ────────────────────────────────────

    @Test
    void handleAsyncRequestNotUsable_doesNotThrow() {
        AsyncRequestNotUsableException ex = new AsyncRequestNotUsableException("Client disconnected", null);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> handler.handleAsyncRequestNotUsable(ex));
    }

    // ── handleGenericException: committed response ─────────────────────

    @Test
    void handleGenericException_returnsNullWhenResponseCommitted() {
        when(response.isCommitted()).thenReturn(true);

        RuntimeException ex = new RuntimeException("Something failed");

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result).isNull();
    }

    // ── handleGenericException: SSE content type ───────────────────────

    @Test
    void handleGenericException_resetsContentTypeForSSE() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("text/event-stream");

        RuntimeException ex = new RuntimeException("SSE failure");

        handler.handleGenericException(ex, request, response);

        verify(response).setContentType("application/json");
    }

    @Test
    void handleGenericException_doesNotResetContentTypeForNonSSE() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        RuntimeException ex = new RuntimeException("JSON failure");

        handler.handleGenericException(ex, request, response);

        verify(response, never()).setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
    }

    // ── handleGenericException: wrapped BaseException ──────────────────

    @Test
    void handleGenericException_withWrappedBaseException_returnsBaseExceptionResponse() {
        stubRequestPath("/test-path");
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        ResourceNotFoundException baseEx = new ResourceNotFoundException("Tournament not found");
        Exception ex = new RuntimeException("Processing failed", baseEx);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(result.getBody().message()).isEqualTo("Tournament not found");
    }

    @Test
    void handleGenericException_withDeeplyNestedBaseException() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("text/html");

        DuplicateResourceException baseEx = new DuplicateResourceException("email duplicado");
        Exception ex = new RuntimeException("outer",
                new RuntimeException("middle",
                        new RuntimeException("inner", baseEx)));

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
    }

    // ── handleGenericException: wrapped IllegalArgumentException ───────

    @Test
    void handleGenericException_withWrappedIllegalArgumentException() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        IllegalArgumentException illegalArg = new IllegalArgumentException("Tournament not found");
        Exception ex = new RuntimeException("Processing failed", illegalArg);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(result.getBody().message()).isEqualTo("No se encontró el torneo solicitado.");
    }

    @Test
    void handleGenericException_withWrappedUnmappedIllegalArgumentException() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        IllegalArgumentException illegalArg = new IllegalArgumentException("custom error");
        Exception ex = new RuntimeException("Processing failed", illegalArg);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getBody().message()).isEqualTo("custom error");
    }

    // ── handleGenericException: wrapped IllegalStateException ──────────

    @Test
    void handleGenericException_withWrappedIllegalStateException() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        IllegalStateException illegalState = new IllegalStateException("Tournament status must not be null");
        Exception ex = new RuntimeException("Processing failed", illegalState);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().code()).isEqualTo("BUSINESS_RULE_CONFLICT");
        assertThat(result.getBody().message()).isEqualTo("Selecciona un estado válido para el torneo.");
    }

    @Test
    void handleGenericException_withWrappedUnmappedIllegalStateException() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        IllegalStateException illegalState = new IllegalStateException("some business issue");
        Exception ex = new RuntimeException("Processing failed", illegalState);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getBody().message()).isEqualTo("some business issue");
    }

    // ── handleGenericException: generic (no special cause) ─────────────

    @Test
    void handleGenericException_withPlainExceptionReturns500() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        RuntimeException ex = new RuntimeException("Unexpected failure");

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(result.getBody().message()).isEqualTo(
                "No se pudo completar la operación. Inténtalo de nuevo más tarde.");
    }

    @Test
    void handleGenericException_withNullCauseReturns500() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn(null);

        RuntimeException ex = new RuntimeException((Throwable) null);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    // ── handleGenericException: BaseException takes priority over IAE/ISE

    @Test
    void handleGenericException_baseExceptionTakesPriorityOverIllegalArgument() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        BaseException baseEx = new ResourceNotFoundException("Tournament not found");
        Exception ex = new RuntimeException("wrapper", baseEx);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ── normalizeValidationMessage coverage ────────────────────────────

    @Test
    void normalizeValidationMessage_knownMappings() {
        stubRequestPath("/test-path");

        org.springframework.validation.FieldError nameEmpty =
                new org.springframework.validation.FieldError("dto", "name", "name must not be empty");
        org.springframework.validation.FieldError nameNull =
                new org.springframework.validation.FieldError("dto", "name", "name must not be null");
        org.springframework.validation.FieldError playPeriod =
                new org.springframework.validation.FieldError("dto", "playPeriod", "playPeriod must not be null");
        org.springframework.validation.FieldError inscriptionPeriod =
                new org.springframework.validation.FieldError("dto", "inscriptionPeriod", "inscriptionPeriod must not be null");
        org.springframework.validation.FieldError maxPlayersNull =
                new org.springframework.validation.FieldError("dto", "maxPlayers", "maxPlayers must not be null");
        org.springframework.validation.FieldError maxPlayersZero =
                new org.springframework.validation.FieldError("dto", "maxPlayers", "maxPlayers must be greater than 0");
        org.springframework.validation.FieldError location =
                new org.springframework.validation.FieldError("dto", "location", "location must not be null");
        org.springframework.validation.FieldError newEvents =
                new org.springframework.validation.FieldError("dto", "newEvents", "newEvents must not be null");
        org.springframework.validation.FieldError duplicateTuple =
                new org.springframework.validation.FieldError("dto", "events", "Duplicate event tuple (categoryId + gender)");
        org.springframework.validation.FieldError genderNull =
                new org.springframework.validation.FieldError("dto", "gender", "gender is null or empty");
        org.springframework.validation.FieldError genderInvalid =
                new org.springframework.validation.FieldError("dto", "gender", "gender must be 'MALE', 'FEMALE' or 'MIXED'");
        org.springframework.validation.FieldError genderInvalidEs =
                new org.springframework.validation.FieldError("dto", "gender", "gender debe ser MALE, FEMALE o MIXED");
        org.springframework.validation.FieldError categoryId =
                new org.springframework.validation.FieldError("dto", "categoryId", "categoryId is null or negative");
        org.springframework.validation.FieldError stageNumber =
                new org.springframework.validation.FieldError("dto", "stageNumber", "stageNumber must be greater than 0");
        org.springframework.validation.FieldError stageType =
                new org.springframework.validation.FieldError("dto", "stageType", "stageType must not be null");
        org.springframework.validation.FieldError drawType =
                new org.springframework.validation.FieldError("dto", "drawType", "drawType must not be null");
        org.springframework.validation.FieldError tournamentNotFound =
                new org.springframework.validation.FieldError("dto", "tournament", "Tournament not found");
        org.springframework.validation.FieldError eventNotFound =
                new org.springframework.validation.FieldError("dto", "event", "Event not found in tournament");
        org.springframework.validation.FieldError noInscriptions =
                new org.springframework.validation.FieldError("dto", "inscriptions", "No inscriptions found for event");

        MethodArgumentNotValidException ex = createValidationException(
                nameEmpty, nameNull, playPeriod, inscriptionPeriod,
                maxPlayersNull, maxPlayersZero, location, newEvents,
                duplicateTuple, genderNull, genderInvalid, genderInvalidEs,
                categoryId, stageNumber, stageType, drawType,
                tournamentNotFound, eventNotFound, noInscriptions
        );

        ResponseEntity<ErrorResponse> result = handler.handleMethodArgumentNotValid(ex, request);

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> details = (java.util.Map<String, String>) result.getBody().details();
        assertThat(details).containsEntry("name", "El nombre del torneo es obligatorio.");
        assertThat(details).containsEntry("playPeriod", "Las fechas de juego son obligatorias.");
        assertThat(details).containsEntry("inscriptionPeriod", "Las fechas de inscripción son obligatorias.");
        assertThat(details).containsEntry("maxPlayers", "El número máximo de jugadores debe ser mayor que cero.");
        assertThat(details).containsEntry("location", "La ubicación del torneo es obligatoria.");
        assertThat(details).containsEntry("newEvents", "Debes enviar al menos un evento válido.");
        assertThat(details).containsEntry("events", "No puedes repetir la misma categoría y género en un torneo.");
        assertThat(details).containsEntry("gender", "El género debe ser masculino, femenino o mixto.");
        assertThat(details).containsEntry("categoryId", "La categoría del evento es obligatoria.");
        assertThat(details).containsEntry("stageNumber", "El número de fase debe ser mayor que cero.");
        assertThat(details).containsEntry("stageType", "El tipo de fase es obligatorio.");
        assertThat(details).containsEntry("drawType", "El tipo de cuadro es obligatorio.");
        assertThat(details).containsEntry("tournament", "No se encontró el torneo solicitado.");
        assertThat(details).containsEntry("event", "No se encontró el evento dentro del torneo.");
        assertThat(details).containsEntry("inscriptions", "Este evento todavía no tiene jugadores inscritos.");
    }

    @Test
    void normalizeValidationMessage_maxPlayersNullTakesPrecedence() {
        stubRequestPath("/test-path");

        org.springframework.validation.FieldError maxPlayersNull =
                new org.springframework.validation.FieldError("dto", "maxPlayers", "maxPlayers must not be null");

        MethodArgumentNotValidException ex = createValidationException(maxPlayersNull);

        ResponseEntity<ErrorResponse> result = handler.handleMethodArgumentNotValid(ex, request);

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> details = (java.util.Map<String, String>) result.getBody().details();
        assertThat(details).containsEntry("maxPlayers", "El número máximo de jugadores es obligatorio.");
    }

    @Test
    void normalizeValidationMessage_genderInvalidEsMapping() {
        stubRequestPath("/test-path");

        org.springframework.validation.FieldError genderInvalidEs =
                new org.springframework.validation.FieldError("dto", "gender", "gender debe ser MALE, FEMALE o MIXED");

        MethodArgumentNotValidException ex = createValidationException(genderInvalidEs);

        ResponseEntity<ErrorResponse> result = handler.handleMethodArgumentNotValid(ex, request);

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> details = (java.util.Map<String, String>) result.getBody().details();
        assertThat(details).containsEntry("gender", "El género debe ser masculino, femenino o mixto.");
    }

    // ── handleGenericException: baseException deep in cause chain ──────

    @Test
    void handleGenericException_baseExceptionAtThirdLevelCause() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        ResourceNotFoundException baseEx = new ResourceNotFoundException("Tournament not found");
        Exception level1 = new RuntimeException("level1", baseEx);
        Exception level2 = new RuntimeException("level2", level1);
        Exception level3 = new RuntimeException("level3", level2);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(level3, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ── handleGenericException: null content type (not SSE) ────────────

    @Test
    void handleGenericException_withNullContentTypeDoesNotReset() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn(null);

        RuntimeException ex = new RuntimeException("failure");

        handler.handleGenericException(ex, request, response);

        verify(response, never()).setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
    }

    // ── handleIllegalArgument: mapped English messages ─────────────────

    @Test
    void handleIllegalArgument_withMappedEnglishMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Tournament not found");

        ResponseEntity<ErrorResponse> result = handler.handleIllegalArgument(ex, request);

        assertThat(result.getBody().message()).isEqualTo("No se encontró el torneo solicitado.");
    }

    @Test
    void handleIllegalArgument_withMappedEventMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Event not found in tournament");

        ResponseEntity<ErrorResponse> result = handler.handleIllegalArgument(ex, request);

        assertThat(result.getBody().message()).isEqualTo("No se encontró el evento dentro del torneo.");
    }

    // ── handleIllegalState: mapped messages ────────────────────────────

    @Test
    void handleIllegalState_withNoInscriptionsMessage() {
        IllegalStateException ex = new IllegalStateException("No inscriptions found for event");

        ResponseEntity<ErrorResponse> result = handler.handleIllegalState(ex, request);

        assertThat(result.getBody().message()).isEqualTo("Este evento todavía no tiene jugadores inscritos.");
    }

    @Test
    void handleIllegalState_withNullMessage() {
        IllegalStateException ex = new IllegalStateException((String) null);

        ResponseEntity<ErrorResponse> result = handler.handleIllegalState(ex, request);

        assertThat(result.getBody().message()).isEqualTo("Revisa los datos enviados antes de continuar.");
    }

    @Test
    void handleIllegalState_withBlankMessage() {
        IllegalStateException ex = new IllegalStateException("   ");

        ResponseEntity<ErrorResponse> result = handler.handleIllegalState(ex, request);

        assertThat(result.getBody().message()).isEqualTo("Revisa los datos enviados antes de continuar.");
    }

    // ── handleGenericException: IAE at deep level ──────────────────────

    @Test
    void handleGenericException_illegalArgumentExceptionAtSecondLevel() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        IllegalArgumentException illegalArg = new IllegalArgumentException("Tournament not found");
        Exception level1 = new RuntimeException("level1", illegalArg);
        Exception level2 = new RuntimeException("level2", level1);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(level2, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void handleGenericException_illegalStateExceptionAtSecondLevel() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        IllegalStateException illegalState = new IllegalStateException("Tournament status must not be null");
        Exception level1 = new RuntimeException("level1", illegalState);
        Exception level2 = new RuntimeException("level2", level1);

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(level2, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().code()).isEqualTo("BUSINESS_RULE_CONFLICT");
    }

    // ── path extraction ────────────────────────────────────────────────

    @Test
    void extractPath_stripsUriPrefix() {
        when(request.getDescription(anyBoolean())).thenReturn("uri=/api/tournaments");

        ResourceNotFoundException ex = new ResourceNotFoundException("Tournament not found");

        ResponseEntity<ErrorResponse> result = handler.handleBaseException(ex, request);

        assertThat(result.getBody().path()).isEqualTo("/api/tournaments");
    }

    @Test
    void handleGenericException_pathFromRequest() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");
        when(request.getDescription(anyBoolean())).thenReturn("uri=/api/players/42");

        RuntimeException ex = new RuntimeException("failure");

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(ex, request, response);

        assertThat(result.getBody().path()).isEqualTo("/api/players/42");
    }

    // ── ErrorResponse body structure ───────────────────────────────────

    @Test
    void errorResponse_hasNullDetailsWhenNotProvided() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Tournament not found");

        ResponseEntity<ErrorResponse> result = handler.handleBaseException(ex, request);

        assertThat(result.getBody().details()).isNull();
        assertThat(result.getBody().timestamp()).isNotNull();
    }

    @Test
    void errorResponse_detailsIsMapForValidationErrors() {
        org.springframework.validation.FieldError fe =
                new org.springframework.validation.FieldError("dto", "name", "name must not be null");

        MethodArgumentNotValidException ex = createValidationException(fe);

        ResponseEntity<ErrorResponse> result = handler.handleMethodArgumentNotValid(ex, request);

        assertThat(result.getBody().details()).isInstanceOf(java.util.Map.class);
    }

    // ── handleGenericException: baseException is preferred over IAE/ISE

    @Test
    void handleGenericException_baseExceptionPrecedesIllegalArgument() {
        when(response.isCommitted()).thenReturn(false);
        when(response.getContentType()).thenReturn("application/json");

        ResourceNotFoundException baseEx = new ResourceNotFoundException("Tournament not found");
        IllegalArgumentException illegalArg = new IllegalArgumentException("should not match");
        RuntimeException wrapper = new RuntimeException("wrapper",
                new RuntimeException("inner", baseEx));

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(wrapper, request, response);

        assertThat(result.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }
}
