package com.erencsahin.coordinator;

import com.erencsahin.calculator.DataCalculator;
import com.erencsahin.dto.Rate;
import com.erencsahin.dto.SubscriberConfig;
import com.erencsahin.subscribers.ISubscriber;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;

@Component
public class Coordinator implements ICoordinator {

    private DataCalculator calculator;
    private ConnectivityService connectivityService;
    public Coordinator(DataCalculator calculator,ConnectivityService connectivityService){
        this.calculator=calculator;
        this.connectivityService=connectivityService;
    }

    @PostConstruct
    public void init(){
        System.out.println("Başlatılıyor.");
        startConnectors();
    }

    public void startConnectors(){
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("subscribers.json");
            if (stream == null) {
                System.out.println("subscribers.json dosyası bulunamadı!");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                Gson gson = new Gson();
                SubscriberConfig[] subscriberConfigs = gson.fromJson(reader, SubscriberConfig[].class);

                for (SubscriberConfig config : subscriberConfigs) {
                    Class<?> clazz = Class.forName(config.getClassName());
                    ISubscriber subscriber = (ISubscriber) clazz.getDeclaredConstructor().newInstance(); //non parameter ctor çağırılır ve nesne oluşturulur.
                    subscriber.init(config, this); //bu şekilde de config dosyası ve Coordinator sınıf parametre olarak geçebiliyoruz.
                    new Thread(subscriber::connect).start();
                }
            }
        } catch (Exception e) {
            System.out.println("Coordinator hata: " + e.getMessage());
            e.getCause();
        }
    }

    @Override
    public void onConnect(String platformName, Boolean status) {
        System.out.println("[CONNECT] " + platformName + ": " + status);
        connectivityService.update(platformName,status);
    }

    @Override
    public void onDisConnect(String platformName, Boolean status) {
        System.out.println("[DISCONNECT] " + platformName + ": " + status);
        connectivityService.update(platformName,status);
    }

    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {
        System.out.println("[DATA] " + platformName + " - " + rate);
        calculator.onNewRate(platformName,rate);
    }
}
