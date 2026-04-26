package com.tfm.tennis_platform.infrastructure.loggin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            UUID correlationId = UUID.randomUUID();
            MDC.put("correlationId", correlationId.toString()); // Set the ID
            request.setAttribute("correlationId", correlationId.toString());
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear(); // Important: Always clear after request ends
        }
    }
}
