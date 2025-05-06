package com.erencsahin.calculator;

import com.erencsahin.dto.Rate;
import com.erencsahin.redis.RedisService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateCalculator {

    private final RedisService redisService;
    private final Map<String, Rate> tcpRaw  = new ConcurrentHashMap<>();
    private final Map<String, Rate> restRaw = new ConcurrentHashMap<>();

    public RateCalculator(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * Coordinator’dan gelen her ham rate’i buraya yönlendir.
     */
    public void onNewRawRate(String platform, Rate rate) {
        // 1) Raw’ı kaydet
        redisService.saveRawRate(platform,rate);

        // 2) Inmemory cache’e al
        String base = rate.getSymbol().substring(rate.getSymbol().indexOf('_') + 1);
        if (platform.equalsIgnoreCase("TcpPlatform")) {
            tcpRaw.put(base, rate);
        } else {
            restRaw.put(base, rate);
        }

        // 3) Ortalamaları calcCross ve calcUsdTry ile hesapla
        calcUsdTry();
        calcCross("EURTRY", "EURUSD");
        calcCross("GBPTRY", "GBPUSD");
    }

    private void calcUsdTry() {
        Rate tcpRate = tcpRaw.get("USDTRY"), restRate = restRaw.get("USDTRY");
        if (tcpRate==null||restRate==null) return;
        double bid = (tcpRate.getBid()+restRate.getBid())/2;
        double ask = (tcpRate.getAsk()+restRate.getAsk())/2;
        String timestamp = restRate.getTimestamp();
        redisService.saveAverageRate(new Rate("USDTRY", ask, bid, timestamp));
    }

    private void calcCross(String cross, String pair) {
        Rate p1 = tcpRaw.get(pair), p2 = restRaw.get(pair), u = restRaw.get("USDTRY");
        if (p1==null||p2==null||u==null) return;
        double usdBid = (u.getBid()+u.getBid())/2;
        double usdAsk = (u.getAsk()+u.getAsk())/2;
        double usdMid = (usdBid+usdAsk)/2;
        double pBid = (p1.getBid()+p2.getBid())/2;
        double pAsk = (p1.getAsk()+p2.getAsk())/2;
        String ts = u.getTimestamp();
        redisService.saveAverageRate(
                new Rate(cross, usdMid*pAsk, usdMid*pBid, ts)
        );
    }
}
