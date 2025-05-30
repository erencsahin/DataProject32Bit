// tcp, rest binding’le geliyor:
var Rate = Java.type("com.erencsahin.dto.Rate");
var bid = (tcp.bid + rest.bid) / 2;
var ask = (tcp.ask + rest.ask) / 2;
new Rate("USDTRY", ask, bid, rest.timestamp);
