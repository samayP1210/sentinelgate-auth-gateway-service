package com.sentinelgate.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoutingUtils {

    @Autowired
    ConfigUtils configUtils;

    // inside your existing ConfigUtils
    public String getServiceHost(String serviceKey) {
        // Simple hardcoded map; replace with externalized config or discovery (Eureka/Consul)
        // Example values could be read from application.properties: gateway.service.product = http://localhost:8081
        if ("product".equals(serviceKey)) return configUtils.getProductServiceHost(); // define productServiceHost field with @Value
        // else return null
        return null;
    }

}
