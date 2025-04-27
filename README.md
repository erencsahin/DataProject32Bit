# FINANSAL VERI SAGLAYICILARDAN ANLIK VERI TOPLAMA VE HESAPLAMA PROJESI

 Bu proje, birden fazla veri sağlayıcıdan (TCP streaming ve REST API) gelen ham Forex kurlarını toplayıp, Redis üzerinde tamponlayarak hem ham hem de türetilmiş (ortalama ve çapraz) kurları hesaplar, Kafka aracılığıyla downstream sistemlere iletir ve PostgreSQL ile OpenSearch’e yazar.

---

## Mimari Özeti

```plain
[TCP Provider]        [REST Provider]
     |                      |
     └─► ISubscriber ──► Coordinator ──► DataCalculator ──► RedisService
                                            │
                                            └─► KafkaProducer ──► Kafka (avg-data)
                                                                        │
                                                                        ▼
                                                              KafkaConsumer
                                                              ├─► PostgreSQL (JPA)
                                                              └─► OpenSearch (RESTHL Client)

Subscribers

    TCPSubscriber: Socket üzerinden subscribe-PF1_USDTRY gibi komutlarla stream’ten veri alır.

    RestSubscriber: Her saniye HTTP GET ile /api/rates?symbol=... çağırır.

Coordinator

    Runtime’da subscribers.json’dan abone sınıflarını (ISubscriber) yükler, her birini ayrı bir thread’de başlatır.

    Gelen her Rate(symbol, bid, ask, timestamp) nesnesini DataCalculator’a iletir.

DataCalculator & RateCalculator

    Ham TCP ve REST verilerini RedisService ile Redis’e yazar.

    calcUsdTry, calcCross("EURTRY","EURUSD"), calcCross("GBPTRY","GBPUSD") yöntemleriyle:

    USDTRY = (PF1 + PF2) / 2

    EURTRY = USD_mid × EURUSD_mid

    GBPTRY = USD_mid × GBPUSD_mid

    Ortalamaları Redis’in DB2’sine avg:<symbol>:<timestamp> formatıyla kaydeder.

RedisService

    DB0: REST raw verileri

    DB1: TCP raw verileri

    DB2: Hesaplanmış ortalama veriler

KafkaProducer

    Her saniye avg:* anahtarlarını tarar, daha önce yayınlanmamışları avg-data topic’ine JSON olarak gönderir.

KafkaConsumer

    avg-data topic’ini dinler, gelen Rate nesnelerini:

    PostgreSQL: JPA ile TblRates tablosuna kaydeder.

    OpenSearch: OpenSearchService.indexRate(...) ile rates indeksine yazar.


TECH STACK
  -Java 21+

  -Maven 3.6+

  -Docker & Docker-Compose

  -Redis
  -Kafka, Zookeeper
  -PostgreSQL
  -OpenSearch

Başlarken

# Cluster ve bağımlılıkları ayağa kaldır
docker-compose down -v
docker-compose up -d

# Maven ile uygulamayı derle ve çalıştır
./mvnw clean package
java -jar target/coordinator-0.1.0.jar


Test ve Doğrulama




Ham Veri
  -Telnet ile localhost:8081 → subscribe-PF1_USDTRY
  -Postman ile GET http://localhost:8082/api/rates?symbol=PF2_USDTRY

Redis
  redis-cli -p 6379  
  SELECT 2
  KEYS avg:*
  GET avg:USDTRY:2025-...

Kafka
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic avg-data --from-beginning \
  --property print.key=true

OpenSearch Dashboards
  http://localhost:5601 → Discover → index pattern = rates
  Dev Tools:
    GET rates/_search
    {
      "size":10,
      "sort":[{"rateTime":{"order":"desc"}}]
    }
