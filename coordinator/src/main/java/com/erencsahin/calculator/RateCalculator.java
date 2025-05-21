package com.erencsahin.calculator;

import com.erencsahin.dto.Rate;
import com.erencsahin.redis.RedisService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateCalculator {
    private final static Logger logger= LogManager.getLogger(RateCalculator.class);
    private final RedisService redisService;
    private final Map<String, Rate> tcpRaw  = new ConcurrentHashMap<>();
    private final Map<String, Rate> restRaw = new ConcurrentHashMap<>();

    public RateCalculator(RedisService redisService) {
        this.redisService = redisService;
        logger.info("RateCalculator oluşturuldu.");
    }

    /**
     * Coordinator’dan gelen her ham rate’i buraya yönlendir.
     */
    public void onNewRawRate(String platform, Rate rate) {
        logger.debug("[onNewRawRate] {} -> {}",platform,rate);
        try {
            redisService.saveRawRate(platform,rate);
            logger.info("Raw rate redis'e kaydedildi: {} - {}",platform,rate);
        }catch (Exception e){
            logger.error("Raw rate redis'e kaydedilemedi: {} - {}",platform,rate);
        }

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
        if (tcpRate==null||restRate==null) {
            logger.debug("USDTRY hesaplama atlandı: tcpRate {} ve restRate {} null geldi.",tcpRate,restRate);
            return;
        }

        double bid = (tcpRate.getBid()+restRate.getBid())/2;
        double ask = (tcpRate.getAsk()+restRate.getAsk())/2;
        String timestamp = restRate.getTimestamp();
        try {
            redisService.saveAverageRate(new Rate("USDTRY", ask, bid, timestamp));
            logger.info("USDTRY raw rate hesaplandı ve redisDb2 ye kaydedildi : ask -> {}, bid -> {}, timestamp -> {}",ask,bid,timestamp);
        }catch (Exception e){
            logger.info("USDTRY kaydedilirken hata meydana geldi.",e);
        }
    }

    private void calcCross(String cross, String pair) {
        Rate p1 = tcpRaw.get(pair), p2 = restRaw.get(pair), u = restRaw.get("USDTRY");
        if (p1==null||p2==null||u==null) {
            logger.debug("{} hesaplama atlandı: pair1 {} ve pair2 {} null geldi.",cross,p1,p2);
            return;
        }

        double usdBid = (u.getBid()+u.getBid())/2;
        double usdAsk = (u.getAsk()+u.getAsk())/2;
        double usdMid = (usdBid+usdAsk)/2;
        double pBid = (p1.getBid()+p2.getBid())/2;
        double pAsk = (p1.getAsk()+p2.getAsk())/2;
        String timestamp = u.getTimestamp();
        try {
            Rate calcRate=new Rate(cross,usdMid*pAsk,usdMid*pBid,timestamp);
            redisService.saveAverageRate(calcRate);
            logger.info("{} ortalama hesaplandı: ask->{}, bid->{}, timestamp->{}",cross,calcRate.getAsk(),calcRate.getBid(),calcRate.getTimestamp());
        }catch (Exception e){
            logger.error("{} ortalama hesaplanamadi.",cross,e);
        }
    }
}
