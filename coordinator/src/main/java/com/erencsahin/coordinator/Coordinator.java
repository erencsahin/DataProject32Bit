package com.erencsahin.coordinator;

import com.erencsahin.calculator.DataCalculator;
import com.erencsahin.dto.Rate;
import com.erencsahin.dto.SubscriberConfig;
import com.erencsahin.subscribers.ISubscriber;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;

@Component
public class Coordinator implements ICoordinator {
    private static final Logger logger= LogManager.getLogger(Coordinator.class);
    private DataCalculator calculator;
    private ConnectivityService connectivityService;
    public Coordinator(DataCalculator calculator,ConnectivityService connectivityService){
        this.calculator=calculator;
        this.connectivityService=connectivityService;
    }

    @PostConstruct
    public void init(){
        logger.info("Coordinator baslatiliyor.");
        startConnectors();
    }

    public void startConnectors(){
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("subscribers.json");
            if (stream == null) {
                logger.error("subscribers.json dosyası bulunamadi.");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                Gson gson = new Gson();
                SubscriberConfig[] subscriberConfigs = gson.fromJson(reader, SubscriberConfig[].class);
                logger.debug("subscriber.json'dan {} config yüklendi",subscriberConfigs.length);

                for (SubscriberConfig config : subscriberConfigs) {
                    Class<?> clazz = Class.forName(config.getClassName());
                    ISubscriber subscriber = (ISubscriber) clazz.getDeclaredConstructor().newInstance(); //non parameter ctor çağırılır ve nesne oluşturulur.
                    subscriber.init(config, this); //bu şekilde de config dosyası ve Coordinator sınıf parametre olarak geçebiliyoruz.
                    new Thread(subscriber::connect).start();
                    logger.info("Subscriber { } için connect thread başlatıldı."+ config.getClassName());
                }
            }
        } catch (Exception e) {
            logger.error("Thread'ler oluşturulurken hata meydana geldi.",e);
        }
    }

    @Override
    public void onConnect(String platformName, Boolean status) {
        logger.info("[CONNECT] " + platformName + ": " + status);
        connectivityService.update(platformName,status);
    }

    @Override
    public void onDisConnect(String platformName, Boolean status) {
        logger.warn("[DISCONNECT] " + platformName + ": " + status);
        connectivityService.update(platformName,status);
    }

    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {
        logger.info("[DATA] " + platformName + " - " + rate);
        calculator.onNewRate(platformName,rate);
    }
}
