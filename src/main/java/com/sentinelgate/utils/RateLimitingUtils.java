package com.sentinelgate.utils;

import com.sentinelgate.pojo.RateLimitRedisPojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingUtils {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private ConfigUtils configUtils;

    private final ConcurrentHashMap<String, Deque<Long>> globalDequeMap;

    RateLimitingUtils() {
        globalDequeMap = new ConcurrentHashMap<>();
    }

    Logger log = LoggerFactory.getLogger(RateLimitingUtils.class);

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
        if (!configUtils.getRateLimitingEnabled()) {
            return true;
        }

        String RATE_LIMITING_REDIS_KEY = "RLP_" + path;
        RateLimitRedisPojo ratePojo = redisUtils.getOrThrow(RATE_LIMITING_REDIS_KEY, RateLimitRedisPojo.class);
        Integer tokens;
        Integer currentTime = (int) (System.currentTimeMillis() / 3000);
        if (ratePojo == null) {
            tokens = configUtils.getRateLimitingTokenSizePerSecond();
        } else {
            Integer lastRequestTime = ratePojo.getLastRequestTime();
            tokens = Math.min(ratePojo.getCount() + (currentTime - lastRequestTime) * configUtils.getRateLimitingTokenSizePerSecond(), configUtils.getRateLimitingMaxToken());
        }

        log.info("time: {}, count: {}", currentTime, tokens);

        if (tokens <= 0) {
            return false;
        }
        redisUtils.set(RATE_LIMITING_REDIS_KEY, new RateLimitRedisPojo(tokens - 1, currentTime));
        return true;
    }

    private Boolean rateLimitLocally(String path) {
        try {
            if (!configUtils.getRateLimitingEnabled()) {
                return true;
            }

            // sliding window rate limiting
            long currentTime = Instant.now().toEpochMilli();
            Deque<Long> pathDeque = globalDequeMap.computeIfAbsent(path, k -> new ArrayDeque<>());

            synchronized (pathDeque) {
                while (!pathDeque.isEmpty() && pathDeque.peekFirst() < currentTime) {
                    pathDeque.pollFirst();
                }

                int current = pathDeque.size();
                if (current < configUtils.getRateLimitingMaxToken()) {
                    pathDeque.addLast(currentTime + configUtils.getRateLimitingTtlMs());
                    globalDequeMap.put(path, pathDeque);
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
