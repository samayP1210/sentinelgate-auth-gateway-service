package com.sentinelgate.utils;

import com.sentinelgate.pojo.RateLimitRedisPojo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

@Service
public class RateLimitingUtils {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private ConfigUtils configUtils;

    private Deque<Long> globalDeque;

    Logger log = LoggerFactory.getLogger(RateLimitingUtils.class);

    @PostConstruct
    public void init(){
        globalDeque = new ArrayDeque<>();
    }

    public Boolean isRequestAllowed(String path) {
        try {
            return rateLimitUsingRedis(path);
        } catch (Exception e) {
            log.error("Error while rate limiting using redis, err: ", e);
        }
        return rateLimitLocally(path);
    }

    private Boolean rateLimitUsingRedis(String path) throws Exception {
        // token bucket, throw exception if connection to redis not established
        if (! configUtils.getRateLimitingEnabled()){
            return true;
        }

        RateLimitRedisPojo ratePojo = redisUtils.getOrThrow("RLP", RateLimitRedisPojo.class);
        Integer tokens;
        Integer currentTime = (int)(System.currentTimeMillis() / 3000);
        if (ratePojo == null){
            tokens = configUtils.getRateLimitingTokenSizePerSecond();
        } else {
            Integer lastRequestTime = ratePojo.getLastRequestTime();
            tokens = Math.min(ratePojo.getCount() + (currentTime - lastRequestTime) * configUtils.getRateLimitingTokenSizePerSecond(), configUtils.getRateLimitingMaxToken());
        }

        log.info("time: {}, count: {}", currentTime, tokens);

        if (tokens <= 0){
            return false;
        }
        redisUtils.set("RLP", new RateLimitRedisPojo(tokens - 1, currentTime));
        return true;
    }

    private Boolean rateLimitLocally(String path) {
        try {
            if (! configUtils.getRateLimitingEnabled()){
                return true;
            }

            // sliding window rate limiting
            long currentTime = Instant.now().toEpochMilli();
            synchronized (globalDeque) {
                while (!globalDeque.isEmpty() && globalDeque.peekFirst() < currentTime) {
                    globalDeque.pollFirst();
                }

                int current = globalDeque.size();
                if (current < configUtils.getRateLimitingMaxToken()) {
                    globalDeque.addLast(currentTime + configUtils.getRateLimitingTtlMs());
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Error while rate limiting locally, err: ", e);
        }
        return true;
    }
}
