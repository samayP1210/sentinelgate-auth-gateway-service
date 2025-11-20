package com.sentinelgate.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtils {

    Logger log = LoggerFactory.getLogger(RedisUtils.class);

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    public <T> void set(String key, T value){
        try {
            if (StringUtils.isNotEmpty(key))
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
            else
                log.info("Empty redis key");
        }catch (Exception e){
            log.error("Error while saving to redis, key: {}, value: {}, err: ", key, value, e);
        }
    }

    public <T> void set(String key, T value, Integer expiry){
        try {
            if (StringUtils.isNotEmpty(key))
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), expiry, TimeUnit.SECONDS);
            else
                log.info("Empty redis key");
        }catch (Exception e){
            log.error("Error while saving to redis, key: {}, value: {}, expiry: {}, err: ", key, value, expiry, e);
        }
    }

    public <T> T get(String key, Class<T> clazz){
        T response = null;
        try {
            if (StringUtils.isNotEmpty(key)){
                String val = redisTemplate.opsForValue().get(key);
                if (val != null)
                    response = objectMapper.readValue(val, clazz);
            } else {
                log.info("Empty redis key");
            }
        }catch (Exception e){
            log.error("Error while getting from redis, key: {}, err: ", key, e);
        }
        log.info("key: {}, value: {}", key, response);
        return response;
    }

    public <T> T getOrThrow(String key, Class<T> clazz) throws Exception {
        if (StringUtils.isNotEmpty(key)) {
            String val = redisTemplate.opsForValue().get(key);
            if (val != null)
                return objectMapper.readValue(val, clazz);
        } else {
            log.info("Empty redis key");
        }
        return null;
    }

}
