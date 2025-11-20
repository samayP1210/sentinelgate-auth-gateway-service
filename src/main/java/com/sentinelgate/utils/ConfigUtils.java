package com.sentinelgate.utils;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ConfigUtils {

    @Value("${redis.host:null}")
    private String redisHost;

    @Value("${redis.port:null}")
    private Integer redisPort;

    @Value("${jwt.secret:null}")
    private String jwtSecret;

    //1 hour in milliseconds
    @Value("${jwt.expiration-ms:3600000}")
    private Long jwtExpirationMs;

    @Value("${rate-limit.token-size-per-second:3}")
    private Integer rateLimitingTokenSizePerSecond;

    @Value("${rate-limit.max-token:7}")
    private Integer rateLimitingMaxToken;

    @Value("${rate-limit.ttl:3600}")
    private Integer rateLimitingTtlMs;

    @Value("${rate-limit.enabled:true}")
    private Boolean rateLimitingEnabled;

}
