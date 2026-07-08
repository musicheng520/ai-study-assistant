package com.msc.springai.dev;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("dev")
@RestController
@RequiredArgsConstructor
public class DevRedisController {

    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/api/dev/redis-ping")
    public Map<String, String> redisPing() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return Map.of("redis", pong);
        }
    }
}