package com.erencsahin.opensearch;

import com.erencsahin.dto.Rate;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Service
public class OpenSearchService {
    private final RestHighLevelClient client;

    public OpenSearchService(RestHighLevelClient client){
        this.client=client;
    }

    public void indexRate(Rate rate){
        try {
            Map<String,Object> rateField=Map.of(
                    "symbol",rate.getSymbol(),
                    "ask",rate.getAsk(),
                    "bid",rate.getBid(),
                    "rateTime",rate.getTimestamp(),
                    "openSearchInsertedTime", Instant.now().toString()
            );
            IndexRequest req=new IndexRequest("rates")
                    .id(rate.getSymbol() + ":"+rate.getTimestamp())
                    .source(rateField);
            client.index(req, RequestOptions.DEFAULT);
        }catch (IOException e){
            throw new RuntimeException("Opensearch'de hata var. "+rate+e);
        }
    }

}
