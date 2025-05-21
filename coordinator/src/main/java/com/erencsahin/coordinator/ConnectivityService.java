package com.erencsahin.coordinator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectivityService {
    //veri sağlayıcı platformların çalışıp çalışmadığını kontrol eden class.
    private final static Logger logger= LogManager.getLogger(ConnectivityService.class);
    private final Set<String> disConnected= ConcurrentHashMap.newKeySet();

    public void update(String platformName,boolean connected){
        if (connected){
            disConnected.remove(platformName);
            logger.info("Platform ayakta. {}",platformName);
        }
        else {
            disConnected.add(platformName);
            logger.debug("Platform ayakta değil. {}",platformName);
        }
    }

    public boolean isHealty(){
        logger.debug("Tüm platformlar saglikli mi kontrolu..");
        return disConnected.isEmpty();
    }
}
