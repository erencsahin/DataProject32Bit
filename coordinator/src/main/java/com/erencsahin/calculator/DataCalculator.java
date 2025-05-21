package com.erencsahin.calculator;

import com.erencsahin.dto.Rate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;


@Service
public class DataCalculator {
    private static final Logger logger= LogManager.getLogger(DataCalculator.class);
    private final RateCalculator rateCalculator;

    public DataCalculator(RateCalculator rateCalculator) {
        this.rateCalculator =rateCalculator;
        logger.info("DataCalculator oluşturuldu.");
    }

    public void onNewRate(String platform, Rate rate) {
        logger.debug("[onNewRate] {} üzerinden yeni rate geldi: {}", platform,rate);
        try {
            rateCalculator.onNewRawRate(platform,rate);
            logger.info("onNewRate {} için veri işlendi.",platform);
        }catch (Exception e){
            logger.error("onNewRate {} için rate işlenirken hata oluştu.",platform,e);
        }
    }
}
