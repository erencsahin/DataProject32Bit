package com.erencsahin.subscribers;

import com.erencsahin.coordinator.ICoordinator;
import com.erencsahin.dto.SubscriberConfig;

public interface ISubscriber {
    void connect();
    /*void disConnect(String platformName,String userName,String password);
    void subscribe(String platformName, String rateName);
    void unSubscribe(String platformName,String rateName);*/
    void init(SubscriberConfig config,ICoordinator coordinator);
   // void start();
}
