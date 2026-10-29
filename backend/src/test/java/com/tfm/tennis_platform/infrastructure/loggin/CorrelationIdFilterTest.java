package com.tfm.tennis_platform.infrastructure.loggin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("Should set correlation ID as request attribute")
    void shouldSetCorrelationId() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute(any(), any(String.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should call filter chain")
    void shouldCallFilterChain() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear MDC after request completes")
    void shouldClearMdcAfterRequest() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Should clear MDC even if filter chain throws exception")
    void shouldClearMdcOnException() throws ServletException, IOException {
        doThrow(new RuntimeException("test")).when(filterChain).doFilter(any(), any());

        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (RuntimeException ignored) {
        }

        assertThat(MDC.get("correlationId")).isNull();
    }
}