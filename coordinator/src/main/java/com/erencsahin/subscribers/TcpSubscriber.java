package com.erencsahin.subscribers;

import com.erencsahin.coordinator.ICoordinator;
import com.erencsahin.dto.Rate;
import com.erencsahin.dto.SubscriberConfig;

import java.io.*;
import java.net.Socket;

//  bu sınıf runtime'da compile edilecek.

public class TcpSubscriber implements ISubscriber {
    public SubscriberConfig subscriberConfig;
    public ICoordinator coordinator;

    @Override
    public void init(SubscriberConfig subscriberConfig, ICoordinator icoordinator){
        this.coordinator = icoordinator;
        this.subscriberConfig=subscriberConfig;
    }

    @Override
    public void connect() {
        System.out.println("Bağlantı deneniyor.");
        try (Socket socket = new Socket(subscriberConfig.getHost(), subscriberConfig.getPort());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            coordinator.onConnect(subscriberConfig.getPlatformName(), true);

            for (String rate : subscriberConfig.getRateList()) {
                out.println("subscribe-" + rate);
            }
            String line;
            while ((line = in.readLine()) != null) {
                processLine(line);
            }
        } catch (IOException e) {
            System.out.println("Bağlantı olmadı.");
            coordinator.onDisConnect(subscriberConfig.getPlatformName(), false);
            e.getCause();
        }
    }

    private void processLine(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length < 4) return;

            String symbol = parts[0].trim();

            String bidString = parts[1].split(":", 2)[1].trim().replace(',', '.');
            String askString = parts[2].split(":", 2)[1].trim().replace(',', '.');

            String timestamp = parts[3].split(":", 2)[1].trim();

            double bid = Double.parseDouble(bidString);
            double ask = Double.parseDouble(askString);

            Rate rate = new Rate(symbol, bid, ask, timestamp);
            coordinator.onRateAvailable(subscriberConfig.getPlatformName(), symbol, rate);
        } catch (Exception e) {
            System.err.println("Veri parse : " + line);
            e.printStackTrace();
        }
    }

}
