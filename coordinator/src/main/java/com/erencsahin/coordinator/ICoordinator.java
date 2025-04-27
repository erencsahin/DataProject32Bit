package com.erencsahin.coordinator;

import com.erencsahin.dto.Rate;

public interface ICoordinator {
    // Bağlantı gerçekleştiğinde çalışacak callback
    void onConnect(String platformName, Boolean status);

    // Bağlantı koptuğunda çalışacak callback
    void onDisConnect(String platformName, Boolean status);

    // istenen veri ilk defa geldiğinde
    void onRateAvailable(String platformName, String rateName, Rate rate);


    // istenen verinin sonraki güncellemeleri
    /*void onRateUpdate(String platformName, String rateName);//, RateFields rateFields);

    // istenen verinin durumu ile ilgili bilgilendime
    void onRateStatus(String platformName, String rateName);//, RateStatus rateStatus);*/
}
