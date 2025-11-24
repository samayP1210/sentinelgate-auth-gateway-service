package com.sentinelgate.contollers;

import com.sentinelgate.manager.GatewayManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Generic gateway controller.
 *
 * Routes requests of the form:
 *   /gateway/{serviceKey}/**
 * to the configured service with key `serviceKey`.
 *
 * Example:
 *   Incoming: GET /gateway/product/123?x=1
 *   -> serviceKey = "product", pathSuffix = "/123?x=1"
 */
@RestController
@RequestMapping("")
public class GatewayController {

    @Autowired
    private GatewayManager gatewayManager;

    /**
     * Generic forwarder: accepts any path under /{serviceKey}/...
     */
    @RequestMapping(value = "/{serviceKey}/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Object> forwardToService(@PathVariable("serviceKey") String serviceKey, HttpServletRequest request) throws IOException {
        // compute pathSuffix (the part after /{serviceKey})
        String prefix = "/" + serviceKey;
        String requestUri = request.getRequestURI();
        String pathSuffix = "";
        if (requestUri.length() > prefix.length()) {
            pathSuffix = requestUri.substring(prefix.length()); // includes leading slash if present
        }

        return gatewayManager.forward(serviceKey, pathSuffix, request);
    }

    /**
     * Root mapping for service key only (no extra suffix).
     * Example: POST /gateway/product  -> pathSuffix = ""
     */
    @RequestMapping(value = "/{serviceKey}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Object> forwardToServiceRoot(@PathVariable("serviceKey") String serviceKey, HttpServletRequest request) throws IOException {
        return gatewayManager.forward(serviceKey, "", request);
    }
}
