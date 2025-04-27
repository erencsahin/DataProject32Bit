package com.erencsahin.calculator;

import com.erencsahin.dto.Rate;
import org.springframework.stereotype.Service;


@Service
public class DataCalculator {
    private final RateCalculator rateCalculator;

    public DataCalculator(RateCalculator rateCalculator) {
        this.rateCalculator =rateCalculator;
    }

    public void onNewRate(String platform, Rate rate) {
        rateCalculator.onNewRawRate(platform, rate);
    }
}
