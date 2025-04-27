package com.erencsahin.subscribers;

import com.erencsahin.coordinator.ICoordinator;
import com.erencsahin.dto.Rate;
import com.erencsahin.dto.SubscriberConfig;
import org.springframework.web.client.RestTemplate;

import java.util.Timer;
import java.util.TimerTask;

//  bu sınıf runtime'da compile edilecek.

public class RestSubscriber implements ISubscriber{
    public SubscriberConfig subscriberConfig;
    public ICoordinator coordinator;
    private final RestTemplate restTemplate=new RestTemplate();

    @Override
    public void init(SubscriberConfig subscriberConfig, ICoordinator coordinator) {
        this.coordinator=coordinator;
        this.subscriberConfig=subscriberConfig;
    }

    @Override
    public void connect() {
        try {
            String testUrl = buildUrl(subscriberConfig.getRateList().get(0));
            System.out.println("REST API bağlantısı test ediliyor: " + testUrl);
            coordinator.onConnect(subscriberConfig.getPlatformName(), true);
        } catch (Exception e) {
            System.err.println("REST API bağlantısı başarısız: " + e.getMessage());
            coordinator.onDisConnect(subscriberConfig.getPlatformName(), false);
            return;
        }

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fetchRates();
            }
        }, 0, 1000);
    }

    private String buildUrl(String symbol) {
        return "http://" +subscriberConfig.getHost()+":"+subscriberConfig.getPort()+"/api/rates?symbol="+symbol;
    }

    public void fetchRates() {
        for (String symbol : subscriberConfig.getRateList()) {
            try {
                String url = buildUrl(symbol);
                Rate rate = restTemplate.getForObject(url, Rate.class);
                coordinator.onRateAvailable(subscriberConfig.getPlatformName(), rate.getSymbol(), rate);

            } catch (Exception e) {
                coordinator.onDisConnect(subscriberConfig.getPlatformName(), false);
                return;
            }
        }
    }
}
