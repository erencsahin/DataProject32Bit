var Rate = Java.type("com.erencsahin.dto.Rate");
var gbpBid = (tcp.bid + rest.bid) / 2;
var gbpAsk = (tcp.ask + rest.ask) / 2;
var usdMid = (usd.bid + usd.ask) / 2;
new Rate("GBPTRY", usdMid * gbpAsk, usdMid * gbpBid, usd.timestamp);
