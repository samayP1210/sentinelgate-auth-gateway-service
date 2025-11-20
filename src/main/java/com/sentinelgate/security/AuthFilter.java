package com.sentinelgate.security;

import com.sentinelgate.utils.JwtUtils;
import com.sentinelgate.utils.RateLimitingUtils;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthFilter extends OncePerRequestFilter {

    @Autowired
    AuthClass authClass;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    RateLimitingUtils rateLimitingUtils;

    Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final AntPathMatcher antPathMatcher = new AntPathMatcher(); // lightweight, safe

    private boolean shouldNotFilter(String path) {
        Set<String> excludePath = authClass.getExcludingPaths();
        return excludePath.stream().noneMatch(pattern -> antPathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (shouldNotFilter(path)) {
            String authHeader = request.getHeader("Authorization");
            String token = "";
            if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring("Bearer ".length()).trim();
            } else if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith("Basic ")) {
                token = authHeader.substring("Basic ".length()).trim();
            }
            if (!jwtUtils.validate(token)) {
                createErrorResponse(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        // apply rate limiting
        if (!rateLimitingUtils.isRequestAllowed(path)) {
            createErrorResponse(response, "Rate limit exceeded", 429);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void createErrorResponse(HttpServletResponse response, String message, Integer status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
