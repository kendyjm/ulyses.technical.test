package com.septeo.ulyses.technical.test.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Servlet filter that logs every HTTP request and its response to a file.
 * Fields logged: request datetime, HTTP method, request URL, response status, processing time (ms).
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        int status = HttpServletResponse.SC_OK;
        try {
            filterChain.doFilter(request, response);
            status = response.getStatus();
        } catch (Throwable t) {
            // The exception is propagating up; the container will set 500 and dispatch
            // to /error, which this filter skips (shouldNotFilterErrorDispatch). At this
            // point response.getStatus() is still the default 200, so record 500 here to
            // avoid logging a failed request as a success.
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("{} {} {} {} {}ms",
                    LocalDateTime.now(),
                    sanitize(request.getMethod()),
                    sanitize(buildUrl(request)),
                    status,
                    duration);
        }
    }

    /**
     * Prevent double-logging: Spring dispatches to /error after an error response,
     * which would trigger this filter a second time without this override.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    /**
     * Builds the logged URL: the request path plus the query string when present,
     * so query-param-driven endpoints (e.g. {@code /api/sales?page=2}) are distinguishable.
     */
    private static String buildUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return query == null ? uri : uri + "?" + query;
    }

    /**
     * Strips CR/LF from attacker-influenced values before they reach the single-line
     * log file, preventing log-line forging via crafted methods or URLs.
     */
    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\r\\n]", "_");
    }
}
