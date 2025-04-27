package com.erencsahin.redis;

import com.erencsahin.dto.Rate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RedisService {

    private final RedisTemplate<String,String> redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // PF1 raw → RedisDb0, PF2 raw → RedisDb1, averages → RedisDb2
    private static final Map<String,Integer> RAW_DB = Map.of(
            "TcpPlatform", 0,
            "RestPlatform",1
    );
    private static final int AVG_DB = 2;

    public RedisService(RedisTemplate<String,String> redis) {
        this.redis = redis;
    }

    public void saveRawRate(String platform, Rate rate) {
        Integer db = RAW_DB.get(platform);
        if (db == null) throw new IllegalArgumentException("Unknown platform: " + platform);

        String symbol = rate.getSymbol().split("_",2)[1];
        String key    = "raw:"  + symbol + ":" + rate.getTimestamp();
        String value  = toJson(rate);

        // Dinamik SELECT ve SET
        redis.execute((RedisCallback<Void>) conn -> {
            conn.select(db);
            conn.set(
                    redis.getStringSerializer().serialize(key),
                    redis.getStringSerializer().serialize(value)
            );
            return null;
        });
    }

    public void saveAverageRate(Rate avg) {
        String key   = "avg:"  + avg.getSymbol() + ":" + avg.getTimestamp();
        String value = toJson(avg);

        redis.execute((RedisCallback<Void>) conn -> {
            conn.select(AVG_DB);
            conn.set(
                    redis.getStringSerializer().serialize(key),
                    redis.getStringSerializer().serialize(value)
            );
            return null;
        });
    }

    private String toJson(Rate r) {
        try {
            return objectMapper.writeValueAsString(r);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialize failed for " + r, e);
        }
    }
}
