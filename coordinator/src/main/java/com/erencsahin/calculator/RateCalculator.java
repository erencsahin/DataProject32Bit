package com.erencsahin.calculator;

import com.erencsahin.calculator.impl.JavaScriptRateCalculator;
import com.erencsahin.dto.Rate;
import com.erencsahin.redis.RedisService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateCalculator {
    private static final Logger logger = LogManager.getLogger(RateCalculator.class);

    private final RedisService redisService;
    private final JavaScriptRateCalculator jsCalc;
    private final Map<String, Rate> tcpRaw  = new ConcurrentHashMap<>();
    private final Map<String, Rate> restRaw = new ConcurrentHashMap<>();

    public RateCalculator(RedisService redisService,
                          JavaScriptRateCalculator jsCalc) {
        this.redisService = redisService;
        this.jsCalc       = jsCalc;
    }

    public void onNewRawRate(String platform, Rate rate) {
        // 1) Raw’ı Redis’e kaydet
        try {
            redisService.saveRawRate(platform, rate);
        } catch (Exception e) {
            logger.error("Raw rate kaydedilemedi", e);
        }
        // 2) In-memory cache
        String base = rate.getSymbol().split("_",2)[1];
        if (platform.equalsIgnoreCase("TcpPlatform")) tcpRaw.put(base, rate);
        else                                        restRaw.put(base, rate);

        // 3) JS script’leri çağır
        calcUsdTry();
        calcCross("EURTRY", "EURUSD");
        calcCross("GBPTRY", "GBPUSD");
    }

    private void calcUsdTry() {
        Rate t = tcpRaw.get("USDTRY"), r = restRaw.get("USDTRY");
        if (t == null || r == null) return;
        Rate avg = jsCalc.calculateFromFile("USDTRY", Map.of("tcp", t, "rest", r));
        redisService.saveAverageRate(avg);
        logger.info("USDTRY hesaplandı via JS: {}", avg);
    }

    private void calcCross(String cross, String pair) {
        Rate p1 = tcpRaw.get(pair), p2 = restRaw.get(pair), u = restRaw.get("USDTRY");
        if (p1 == null || p2 == null || u == null) return;
        Rate avg = jsCalc.calculateFromFile(cross, Map.of("tcp", p1, "rest", p2, "usd", u));
        redisService.saveAverageRate(avg);
        logger.info("{} hesaplandı via JS: {}", cross, avg);
    }
}
