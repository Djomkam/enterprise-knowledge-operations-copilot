package io.innovation.ekoc.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that manages correlation IDs for request tracing.
 * Reads X-Correlation-ID from request headers, generates one if absent,
 * stores it in MDC for logging, and returns it in response headers.
 */
@Slf4j
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Read correlation ID from header or generate a new one
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = generateCorrelationId();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using provided correlation ID: {}", correlationId);
            }

            // Store correlation ID in MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add correlation ID to response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue the filter chain
            chain.doFilter(request, response);
        } finally {
            // Always clean up MDC after request is complete
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Generates a new correlation ID using UUID.
     *
     * @return A unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("Correlation ID filter initialized");
    }

    @Override
    public void destroy() {
        log.info("Correlation ID filter destroyed");
    }
}
