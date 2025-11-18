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
    private long jwtExpirationMs;

}
