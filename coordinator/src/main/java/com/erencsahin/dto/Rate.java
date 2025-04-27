package com.erencsahin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rate {
    private String symbol;
    private double ask;
    private double bid;
    private String timestamp;

    @Override
    public String toString(){
        return symbol+" || bid:"+bid+" || ask:"+ask+" || "+timestamp;
    }
}
