package com.sentinelgate.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelgate.utils.ConfigUtils;
import com.sentinelgate.utils.RoutingUtils;
import com.sentinelgate.utils.WebUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GatewayManager
 * <p>
 * Responsibilities:
 * - Build outbound request (method, headers, querystring, body)
 * - Forward to target service using RestTemplate
 * - Preserve outbound response status, headers and body
 * - Central place to add cross-cutting concerns (retries, circuit-breaker, metrics, auth, rate-limiting)
 */
@Service
public class GatewayManager {

    private final Logger log = LoggerFactory.getLogger(GatewayManager.class);

    @Autowired
    private WebUtils webUtils;

    @Autowired
    private RoutingUtils routingUtils;                 // used for routing config

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigUtils configUtils;

    /**
     * Forward request to a target service.
     *
     * @param serviceKey logical service id (e.g., "product") used to lookup target base URL
     * @param pathSuffix path after service root (can be empty or include path params)
     */
    public ResponseEntity<Object> forward(String serviceKey, String pathSuffix, HttpServletRequest request) throws IOException {
        try {
            Objects.requireNonNull(serviceKey, "serviceKey");

            log.info("Redirecting to {} service", serviceKey);

            String targetBase = routingUtils.getServiceHost(serviceKey);
            if (targetBase == null || targetBase.isBlank()) {
                log.error("No target configured for serviceKey={}", serviceKey);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Service not configured");
            }

            // build targetUrl safely (avoid double slashes)
            String suffix = (pathSuffix == null) ? "" : pathSuffix;
            String query = Optional.ofNullable(request.getQueryString()).map(q -> "?" + q).orElse("");
            String targetUrl = targetBase + "/" + serviceKey + suffix + query;

            HashMap<String, String> headers = webUtils.getHeaders(request); // assume webUtils returns a Map<String,String>
            // determine method (case-insensitive)
            String rawMethod = request.getMethod();
            String method = (rawMethod == null || rawMethod.isBlank()) ? "GET" : rawMethod.toUpperCase(Locale.ROOT);

            // Read body safely (raw bytes -> UTF-8 string). If you need binary support, change WebUtils API.
            String body = null;
            Object bodyObject = new Object();
            if (!"GET".equals(method) && !"DELETE".equals(method)) {
                byte[] bytes = StreamUtils.copyToByteArray(request.getInputStream());
                if (bytes.length > 0) {
                    body = new String(bytes, StandardCharsets.UTF_8);
                    bodyObject = objectMapper.readValue(body, Object.class);
                }
            }

            String upstreamResponse;
            try {
                switch (method) {
                    case "GET":
                        upstreamResponse = webUtils.get(targetUrl, headers, configUtils.getProductServiceTimeout());
                        break;
                    case "PUT":
                        upstreamResponse = webUtils.put(targetUrl, bodyObject, headers, configUtils.getProductServiceTimeout());
                        break;
                    case "POST":
                        upstreamResponse = webUtils.post(targetUrl, bodyObject, headers, configUtils.getProductServiceTimeout());
                        break;
                    case "DELETE":
                        upstreamResponse = webUtils.delete(targetUrl, headers, configUtils.getProductServiceTimeout());
                        break;
                    default:
                        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(Map.of("error", "Method not allowed: " + method));
                }
            } catch (Exception ex) {
                log.error("Error calling upstream {} {} -> {}", method, targetUrl, ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Upstream service error", "detail", ex.getMessage()));
            }

            if (upstreamResponse == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            // Try to parse JSON response; if that fails, return raw string
            String trimmed = upstreamResponse.trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                try {
                    Object json = objectMapper.readValue(upstreamResponse, Object.class);
                    return ResponseEntity.ok(json);
                } catch (Exception e) {
                    log.info("Upstream returned JSON-like text but failed to parse; returning raw text. err={}", e.getMessage());
                    return ResponseEntity.ok(upstreamResponse);
                }
            }

            // Non-JSON response: return as plain text
            return ResponseEntity.ok(upstreamResponse);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
