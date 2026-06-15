package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.infrastructure.controller.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldUseWrappedIllegalArgumentMessage() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/tournaments/tournament-id/events/event-id/manual-inscriptions");

        HttpServletResponse response = mock(HttpServletResponse.class);

        Exception exception = new RuntimeException(
                "Request processing failed",
                new IllegalArgumentException("Las inscripciones solo están permitidas cuando el torneo está abierto.")
        );

        ResponseEntity<ErrorResponse> result = handler.handleGenericException(exception, request, response);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("INVALID_REQUEST", result.getBody().code());
        assertEquals("Las inscripciones solo están permitidas cuando el torneo está abierto.", result.getBody().message());
    }
}
