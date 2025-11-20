package com.sentinelgate.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;


@Component
public class JwtUtils {

    @Autowired
    private ConfigUtils configUtils;

    @Autowired
    private ObjectMapper objectMapper;

    Logger log = LoggerFactory.getLogger(JwtUtils.class);

    public String generateToken(Object object) {
        try {
            return Jwts.builder()
                    .setSubject(objectMapper.writeValueAsString(object))
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000L))
                    .signWith(getKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Error while generating token: {}, err: ", object, e);
        }
        return null;
    }

    private Key getKey() {
        byte[] key = Decoders.BASE64.decode(configUtils.getJwtSecret());
        return Keys.hmacShaKeyFor(key);
    }

    public <T> T extractPayload(String token, Class<T> clazz) {
        try {
            String subject = extractClaim(token, Claims::getSubject);
            return objectMapper.readValue(subject, clazz);
        } catch (Exception e) {
            log.error("Error while extracting payload, err: ", e);
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimClass) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token).getBody();
        return claimClass.apply(claims);
    }

    public boolean validate(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (Exception e) {
            log.error("Error while validating token, err: ", e);
            return false;
        }
    }

}
