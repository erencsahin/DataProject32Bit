package com.erencsahin.kafka;

import com.erencsahin.coordinator.ConnectivityService;
import com.erencsahin.dto.Rate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KafkaProducer {
    private final static Logger logger= LogManager.getLogger(KafkaProducer.class);
    private final RedisTemplate<String,String> redisTemplate;
    private final KafkaTemplate<String,Rate> kafkaTemplate;
    private final ConnectivityService connectivity;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> publishedKeys = ConcurrentHashMap.newKeySet();
    private final String kafkaTopic;
    private final JavaMailSender mailSender;
    @Value("${app.mail.to}")
    private String mailTo;

    @Value("${app.mail.from}")
    private String mailFrom;


    //kafka topic'ine veriyi yollayan class.

    public KafkaProducer(
            RedisTemplate<String,String> redisTemplate,
            KafkaTemplate<String,Rate> kafkaTemplate,
            ConnectivityService connectivityService,
            JavaMailSender mailSender,
            @Value("${app.kafka.topic.avg-data:avg-data}") String kafkaTopic
    ) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopic= kafkaTopic;
        this.mailSender=mailSender;
        this.connectivity=connectivityService;
    }

    //her 1 sn'de db2'de 'avg:*' formatına uygun key'leri kontrol edip publish edilmeyenleri kafkaya yollayan fonskiyon.
    @Scheduled(fixedDelayString = "${app.redis.poll-interval:2000}")
    public void sendAvg() {
        try {
            // 1) Platformların sağlıklı çalışıp çalışmadığını kontrol et
            if (!connectivity.isHealty()) {
                String subject = "[UYARI] Platform Bağlantısı Kesildi";
                String messageBody = "KafkaProducer.sendAvg():\n" +
                        "ConnectivityService.isHealty() false döndü. " +
                        "Platformlardan en az bir tanesi offline durumda. " +
                        "Bu nedenle veri akışı sağlanamayacak.";

                // E-posta gönderimini gerçekleştir
                sendEmailNotification(subject, messageBody);

                // Logla ve metottan çık
                logger.error("Platformlardan birisi offline, kafkaya veri gönderilmeyecek.");
                return;
            } else {
                logger.info("Tüm veri sağlayıcı platformlar sağlıklı, avg verisi gönderilmeye başlanıyor.");
            }

            // 2) Redis DB2'yi seç ve avg:* anahtarlarını güncelle
            redisTemplate.execute((RedisCallback<Void>) conn -> {
                conn.select(2);

                // Tüm "avg:*" anahtarlarını al
                Set<byte[]> rawKeys = conn.keys(redisTemplate.getStringSerializer().serialize("avg:*"));
                if (rawKeys == null) {
                    logger.debug("avg:* pattern'ine uygun anahtar bulunamadı.");
                    return null;
                }

                for (byte[] rawKey : rawKeys) {
                    String key = redisTemplate.getStringSerializer().deserialize(rawKey);

                    // Eğer daha önce publish edilmişse atla
                    if (!publishedKeys.add(key)) {
                        logger.trace("Zaten yayınlanmış olan anahtar es geçildi => {}", key);
                        continue;
                    }

                    // Değeri oku, JSON parse et, Kafka'ya yolla
                    byte[] valueByte = conn.get(rawKey);
                    String json = redisTemplate.getStringSerializer().deserialize(valueByte);
                    try {
                        Rate avg = objectMapper.readValue(json, Rate.class);
                        kafkaTemplate.send(kafkaTopic, avg.getSymbol(), avg);
                        logger.debug("Published {} to topic {}", avg, kafkaTopic);
                    } catch (Exception e) {
                        logger.error("Failed to deserialize or send avg for key {}", key, e);
                    }
                }
                return null;
            });

        } catch (Exception ex) {
            // Beklenmeyen bir hata meydana geldiyse, e-posta ile bilgilendirme yap
            String subject = "[HATA] sendAvg() esnasında beklenmeyen istisna";
            String messageBody = "sendAvg() metodu çalışırken aşağıdaki exception fırladı:\n\n"
                    + ex.getClass().getName() + ": " + ex.getMessage();

            // Stack trace'i kısaca loglayabilir veya full trace'i string’e çevirip e-postada paylaşabilirsiniz.
            logger.error("sendAvg() metodu çalışırken hata oluştu.", ex);
            sendEmailNotification(subject, messageBody);
        }
    }


    private void sendEmailNotification(String subject, String messageBody){
        try {
            SimpleMailMessage message=new SimpleMailMessage();
            if (mailFrom!=null && !mailFrom.isBlank()){
                message.setFrom(mailFrom);
            }
            message.setTo(mailTo);
            message.setSubject(subject);
            message.setText(messageBody);

            mailSender.send(message);
            logger.info("Bilgilendirme e-postası yollandı: {}",subject);
        }catch (Exception e){
            logger.error("e-posta gönderilemedi. {}",e.getMessage(),e);
        }
    }
}
