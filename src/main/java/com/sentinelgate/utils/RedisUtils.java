package com.sentinelgate.utils;

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
    RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value){
        try {
            if (StringUtils.isNotEmpty(key))
                redisTemplate.opsForValue().set(key, value);
            else
                log.info("Empty redis key");
        }catch (Exception e){
            log.error("Error while saving to redis, key: {}, value: {}, err: ", key, value, e);
        }
    }

    public void set(String key, Object value, Integer expiry){
        try {
            if (StringUtils.isNotEmpty(key))
                redisTemplate.opsForValue().set(key, value, expiry, TimeUnit.SECONDS);
            else
                log.info("Empty redis key");
        }catch (Exception e){
            log.error("Error while saving to redis, key: {}, value: {}, expiry: {}, err: ", key, value, expiry, e);
        }
    }

    public Object get(String key){
        Object val = null;
        try {
            if (StringUtils.isNotEmpty(key))
                val = redisTemplate.opsForValue().get(key);
            log.info("Empty redis key");
        }catch (Exception e){
            log.error("Error while getting from redis, key: {}, err: ", key, e);
        }
        log.info("key: {}, value: {}", key, val);
        return val;
    }

}
