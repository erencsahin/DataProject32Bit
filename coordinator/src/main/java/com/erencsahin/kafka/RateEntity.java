package com.erencsahin.kafka;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateEntity {
    @Id
    @GeneratedValue
    public Long id;

    @Column(name="symbol")
    public String symbol;

    public double ask;
    public double bid;

    @Column(name = "rateUpdateTime")
    public LocalDateTime rateTime;

    @Column(name = "dbInsertedTime")
    public LocalDateTime dbTime = LocalDateTime.now();
}
