# FINANSAL VERI SAGLAYICILARDAN ANLIK VERI TOPLAMA VE HESAPLAMA PROJESI

Bu proje, birden fazla veri sağlayıcıdan (TCP streaming ve REST API) gelen ham Forex kurlarını toplayıp, Redis üzerinde tamponlayarak hem ham hem de türetilmiş (ortalama ve çapraz) kurları hesaplar, Kafka aracılığıyla downstream sistemlere iletir ve PostgreSQL ile OpenSearch’e yazar.
## Mimari Özeti
![file_2025-06-07_12 13 52 1](https://github.com/user-attachments/assets/2871dc95-e22b-4377-bac3-249c6c47de2f)

## **Tech Stack**
- Java 21+
- Maven 3.6+
- Docker & Docker-Compose
- Redis
- Kafka, Zookeeper
- PostgreSQL
- OpenSearch

## **Submodules**
- **coordinator**
- **kafka-consumer-postgresql**
- **kafka-consumer-opensearch**
- **tcp**
- **rest**

## **coordinator submodule**
- Subscriber sınıfları dinamik olarak yükler ve yönetir.
- Sınıfların yüklenmesi için gerekli olan tüm bilgiler konfigurasyon dosyasında bulunur.
- Subscriber sınıflardan gelen verileri dinamik olarak hesaplar.
- Ham ve hesaplanmış verileri farklı Redis kümelerine yazar.
- KafkaProducer burada yer alır.

## **kafka-consumer-postgresql**
- Yalnızca PostgreSQL veri aktarımıyla sorumlu olan KafkaConsumer burada yer alır.
- Verinin %1’lik dalgalanmaları burada filtrelenir.
- Yeni gelen veri eski veriden %1 az veya fazla ise bu veri esgeçilir ve eski veri yazılır.

## **kafka-consumer-opensearch**
- Yalnızca OpenSearch veri aktarımıyla sorumlu olan KafkaConsumer burada yer alır.
- Verinin %1’lik dalgalanmaları burada filtrelenir.
- Yeni gelen veri eski veriden %1 az veya fazla ise bu veri esgeçilir ve veri yazılmaz.

## **tcp (PF_1)**
- TCP protokolü ile streaming veri sağlayıcı simülatörünü barındırır veya tüketir.
- Gerçekçi veri üretiminden sorumludur.

## **rest (PF_2)**
- REST API üzerinden periyodik rate sağlama.
- Gerçekçi veri üretiminden sorumludur.


## Kurulum ve Çalıştırma

### Otomatik Kurulum ve Başlatma

Sağlanan startup.sh betiği ile sistemi kolayca başlatabilirsiniz:

```bash
git clone --recurse-submodules https://github.com/erencsahin/DataProject32Bit.git
cd DataProject32Bit
```

```bash
### Modülleri derle
mvn clean package -DskipTests
```

```bash
# Docker ile ayağa kaldır
docker-compose up -d --build
```


## **Portlar**
- tcp → http://localhost:8081
- rest → http://localhost:8082/api/rates
- redis → http://localhost:6379
- opensearchDashboard → http://localhost:5601
- kafka → http://localhost:9092
- postgres → http://localhost:5432
- pgadmin → http://localhost:5050

## **Kontrol**

**Redis**
- redis-cli -h localhost -p 6379
- SELECT 2
- KEYS avg:*
- GET avg:USDTRY:2025-06-09T12:00:00.000

**PostgreSQL** (PgAdmin ile)
- Tarayıcıdan http://localhost:5050 adresine gidiniz.
  -Username: admin@admin.com
  -Password: admin

- Sol kısımdan Register → Server
    - Aşağıdaki bilgileri girin:
        - Hostname: postgres
        - Port: 5432
        - Maintenance database: toyotadb
        - Username: toyota
        - Password: secret
          Sol menüden toyotadb → Schemas → public → Tables altında tablomuzu görüntüleyebilirsiniz.


**Kafka**
- kafka-console-consumer --bootstrap-server localhost:9092 --topic avg-data --from-beginning


**Opensearch Dashboard**
- http://localhost:5601 adresine gidiniz.
- Index pattern olarak rates ve toyota-logs-* şeklinde 2 adet pattern ekleyin.
- Soldaki menüden Discover’a tıklayıp verilerimizi görebilirsiniz.

## Hesaplamalar ##

### USD/TRY Hesabı
- `USDTRY.bid = (PF1_USDTRY.bid + PF2_USDTRY.bid) / 2`
- `USDTRY.ask = (PF1_USDTRY.ask + PF2_USDTRY.ask) / 2`

### EUR/TRY Hesabı
- EUR/USD kurları ve USD/TRY mid-price değeri kullanılarak hesaplanır.
- `usdmid = ((PF1_USDTRY.bid + PF2_USDTRY.bid) / 2 + (PF1_USDTRY.ask + PF2_USDTRY.ask) / 2) / 2`
- `EURTRY.bid = usdmid × ((PF1_EURUSD.bid + PF2_EURUSD.bid) / 2)`
- `EURTRY.ask = usdmid × ((PF1_EURUSD.ask + PF2_EURUSD.ask) / 2)`

### GBP/TRY Hesabı
- GBP/USD kurları ve USD/TRY mid-price değeri kullanılarak hesaplanır.
- `usdmid = ((PF1_USDTRY.bid + PF2_USDTRY.bid) / 2 + (PF1_USDTRY.ask + PF2_USDTRY.ask) / 2) / 2`
- `GBPTRY.bid = usdmid × ((PF1_GBPUSD.bid + PF2_GBPUSD.bid) / 2)`
- `GBPTRY.ask = usdmid × ((PF1_GBPUSD.ask + PF2_GBPUSD.ask) / 2)`