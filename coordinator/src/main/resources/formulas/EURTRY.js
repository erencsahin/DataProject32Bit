var Rate = Java.type("com.erencsahin.dto.Rate");
var eurBid = (tcp.bid + rest.bid) / 2;
var eurAsk = (tcp.ask + rest.ask) / 2;
var usdMid = (usd.bid + usd.ask) / 2;
new Rate("EURTRY", usdMid * eurAsk, usdMid * eurBid, usd.timestamp);
