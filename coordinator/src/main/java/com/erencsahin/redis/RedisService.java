package com.erencsahin.redis;

import com.erencsahin.dto.Rate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RedisService {
    private final static Logger logger= LogManager.getLogger(RedisService.class);
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
        logger.info("Redis servis oluşturuldu.");
    }

    public void saveRawRate(String platform, Rate rate) {
        logger.debug("[saveRawRate] çağırıldı. platform -> {}, rate -> {}",platform,rate);
        Integer db = RAW_DB.get(platform);
        if (db == null) {
            logger.error("[saveRawRate] tanımsız platform {}", platform);
            throw new IllegalArgumentException("Unknown platform: " + platform);
        }

        String symbol = rate.getSymbol().split("_",2)[1];
        String key    = "raw:"  + symbol + ":" + rate.getTimestamp();
        String value  = toJson(rate);

        // Dinamik SELECT ve SET
        try {
            redis.execute((RedisCallback<Void>) conn -> {
                conn.select(db);
                conn.set(
                        redis.getStringSerializer().serialize(key),
                        redis.getStringSerializer().serialize(value)
                );
                return null;
            });
            logger.info("[saveRawRate] RedisDb{}’ye kaydedildi → key={}", db, key);
        }catch (Exception e){
            logger.error("[saveRawRate] Redis’e kaydetme hatası → platform={}, key={}", platform, key, e);
        }

    }

    public void saveAverageRate(Rate avg) {
        logger.debug("[saveAverageRate] Çağrıldı → avg={}", avg);
        String key= "avg:"  + avg.getSymbol() + ":" + avg.getTimestamp();
        String value = toJson(avg);

        try {
            redis.execute((RedisCallback<Void>) conn -> {
                conn.select(AVG_DB);
                conn.set(
                        redis.getStringSerializer().serialize(key),
                        redis.getStringSerializer().serialize(value)
                );
                return null;
            });
            logger.info("[saveAverageRate] RedisDb{}’ye kaydedildi → key={}", AVG_DB, key);
        } catch (Exception e) {
            logger.error("[saveAverageRate] Redis’e kaydetme hatası → key={}", key, e);
        }
    }

    private String toJson(Rate rate) {
        logger.trace("[toJson] JSON’a dönüştürülüyor → rate={}", rate);
        try {
            return objectMapper.writeValueAsString(rate);
        } catch (Exception e) {
            logger.error("[toJson] JSON serialize hatası → rate={}", rate, e);
            throw new IllegalStateException("JSON serialize fail " + rate, e);
        }
    }
}
