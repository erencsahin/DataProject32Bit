package com.erencsahin.kafka;


import com.erencsahin.dto.Rate;
import com.erencsahin.opensearch.OpenSearchService;
import com.erencsahin.repository.RateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {
    private final RateRepository rateRepository;
    private final OpenSearchService openSearchService;

    @KafkaListener(topics = "avg-data", groupId = "avg-consumers", containerFactory = "kafkaListenerContainerFactory")
    public void consumer(Rate rate){
        RateEntity rateEntity=new RateEntity(
                null,
                rate.getSymbol(),
                rate.getAsk(),
                rate.getBid(),
                LocalDateTime.parse(rate.getTimestamp()),
                LocalDateTime.now()
        );
        rateRepository.save(rateEntity);
        openSearchService.indexRate(rate);
    }

}