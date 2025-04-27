package com.erencsahin.kafka;

import com.erencsahin.dto.Rate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Rate>
    kafkaListenerContainerFactory(ConsumerFactory<String, Rate> cf) {
        ConcurrentKafkaListenerContainerFactory<String, Rate> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);

        //default olarak 5 sn'de bir olarak verileri kontrol ediyordu ancak devamlı bir veri akışı olmasını istememiz sebebiyle 1 sn'ye olarak config ettim.
        factory.getContainerProperties().setPollTimeout(1000);

        return factory;
    }
}
