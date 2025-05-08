package com.erencsahin.kafka;

import com.erencsahin.coordinator.ConnectivityService;
import com.erencsahin.dto.Rate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class KafkaProducer {

    private final RedisTemplate<String,String> redisTemplate;
    private final KafkaTemplate<String,Rate> kafkaTemplate;
    private final ConnectivityService connectivity;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> publishedKeys = ConcurrentHashMap.newKeySet();
    private final String kafkaTopic;

    //kafka topic'ine veriyi yollayan class.

    public KafkaProducer(
            RedisTemplate<String,String> redisTemplate,
            KafkaTemplate<String,Rate> kafkaTemplate,
            ConnectivityService connectivityService,
            @Value("${app.kafka.topic.avg-data:avg-data}") String kafkaTopic
    ) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopic= kafkaTopic;
        this.connectivity=connectivityService;
    }

    //her 1 sn'de db2'de 'avg:*' formatına uygun key'leri kontrol edip publish edilmeyenleri kafkaya yollayan fonskiyon.
    @Scheduled(fixedDelayString = "${app.redis.poll-interval:1000}")
    public void sendAvg() {
        if (!connectivity.isHealty()) {
            log.debug("Platformlardan birisi calismiyor, kafkaya veri gönderilmeyecek.");
            return;
        }

        redisTemplate.execute((RedisCallback<Void>) conn -> {
            // 2 nolu Redisdb’yi seç
            conn.select(2);

            // tüm avg:* anahtarlarını al
            Set<byte[]> rawKeys = conn.keys(redisTemplate.getStringSerializer()
                    .serialize("avg:*"));
            if (rawKeys == null) return null;

            for (byte[] rawKey : rawKeys) {
                String key = redisTemplate.getStringSerializer().deserialize(rawKey);
                if (!publishedKeys.add(key)) {
                    continue;  // zaten yayınlandı
                }
                // değeri oku -> kafkayaYolla
                byte[] valueByte = conn.get(rawKey);
                String json = redisTemplate.getStringSerializer().deserialize(valueByte);
                try {
                    Rate avg = objectMapper.readValue(json, Rate.class);
                    //yolladığımız yer.
                    kafkaTemplate.send(kafkaTopic, avg.getSymbol(), avg);
                    log.debug("Published {} to topic {}", avg, kafkaTopic);
                } catch (Exception e) {
                    log.error("Failed to deserialize or send avg for key {}", key, e);
                }
            }
            return null;
        });
    }
}
