package com.example.wmbservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiV1DeprecationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiV1DeprecationFilter.class);
    private static final String DEPRECATION_HEADER_VALUE = "true";
    private static final String SUNSET_HEADER_VALUE = "Wed, 31 Dec 2026 23:59:59 GMT";
    private static final String LINK_HEADER_VALUE = "</api/v2>; rel=\"successor-version\"";

    private final ApiV1Properties apiV1Properties;
    private final Counter deprecationCounter;

    public ApiV1DeprecationFilter(ApiV1Properties apiV1Properties, MeterRegistry meterRegistry) {
        this.apiV1Properties = apiV1Properties;
        this.deprecationCounter = Counter.builder("api.v1.deprecation.hits")
                .description("Count of legacy v1 API hits while v1 is in deprecated mode")
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = requestPath(request);
        boolean legacyV1Request = isLegacyV1Request(path);

        if (legacyV1Request && apiV1Properties.getMode() == ApiV1Properties.Mode.DISABLED) {
            logger.info("api.v1.disabled.hit method={} uri={}", request.getMethod(), path);
            response.setStatus(HttpServletResponse.SC_GONE);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Legacy v1 API has been retired. Use /api/v2.");
            return;
        }

        if (legacyV1Request && apiV1Properties.getMode() == ApiV1Properties.Mode.DEPRECATED) {
            deprecationCounter.increment();
            logger.debug("api.v1.deprecation.hit method={} uri={}", request.getMethod(), path);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (legacyV1Request && apiV1Properties.getMode() == ApiV1Properties.Mode.DEPRECATED && !response.isCommitted()) {
                response.setHeader("Deprecation", DEPRECATION_HEADER_VALUE);
                response.setHeader("Sunset", SUNSET_HEADER_VALUE);
                response.setHeader("Link", LINK_HEADER_VALUE);
            }
        }
    }

    private String requestPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private boolean isLegacyV1Request(String uri) {
        return uri != null
                && uri.startsWith("/api/")
                && !uri.equals("/api/v2")
                && !uri.startsWith("/api/v2/");
    }
}
