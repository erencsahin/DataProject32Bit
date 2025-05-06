package com.erencsahin.coordinator;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectivityService {
    private final Set<String> disConnected= ConcurrentHashMap.newKeySet();
    public void update(String platformName,boolean connected){
        if (connected){
            disConnected.remove(platformName);
        }
        else
            disConnected.add(platformName);
    }

    public boolean isHealty(){
        return disConnected.isEmpty();
    }
}
