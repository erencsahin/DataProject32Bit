package com.erencsahin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriberConfig {
    private String className;
    private String platformName;
    private String userName;
    private String password;
    private String host;
    private int port;
    private List<String> rateList;
}
