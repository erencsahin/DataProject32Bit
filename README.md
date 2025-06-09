# FINANSAL VERI SAGLAYICILARDAN ANLIK VERI TOPLAMA VE HESAPLAMA PROJESI

 Bu proje, birden fazla veri sağlayıcıdan (TCP streaming ve REST API) gelen ham Forex kurlarını toplayıp, Redis üzerinde tamponlayarak hem ham hem de türetilmiş (ortalama ve çapraz) kurları hesaplar, Kafka aracılığıyla downstream sistemlere iletir ve PostgreSQL ile OpenSearch’e yazar.

---

## Mimari Özeti
![file_2025-06-07_12 13 52 1](https://github.com/user-attachments/assets/2871dc95-e22b-4377-bac3-249c6c47de2f)
```plain

## Tech Stack
    -Java 21+
    -Maven 3.6+
    -Docker & Docker-Compose
    -Redis
    -Kafka, Zookeeper
    -PostgreSQL
    -OpenSearch 
  
## Submodules
- **coordinator**
- **kafka-consumer-postgresql**
- **kafka-consumer-opensearch**
- **tcp**
- **rest**
##  coordinator submodule

- Subscriber sınıfları dinamik olarak yükler ve yönetir.
- Subscriber sınıflardan gelen verileri dinamik olarak hesaplar.
- Ham ve hesaplanmış verileri farklı redis kümelerine yazar.
- KafkaProducer burada yer alır.
## kafka-consumer-postgresql
- Yalnızca Postgresql veri aktarımıyla sorumlu olan KafkaConsumer burada yer alır.
- Verinin %1'lik dalgalanmaları burada filtrelenir.
- Yeni gelen veri eski veriden %1 az veya fazla ise bu veri esgeçilir ve eski veri yazılır.

## kafka-consumer-opensearch
- Yalnızca Opensearch veri aktarımıyla sorumlu olan KafkaConsumer burada yer alır.
- Verinin %1'lik dalgalanmaları burada filtrelenir.
- Yeni gelen veri eski veriden %1 az veya fazla ise bu veri esgeçilir ve veri yazılmaz.
## tcp (PF_1)
- TCP protokolü ile streaming veri sağlayıcı simülatörünü barındırır veya tüketir.
- Gerçekçi veri üretimden sorumludur.
## rest (PF_2)
- REST API üzerinden periyodik rate sağlama.
- Gerçekçi veri üretimden sorumludur
## Kurulum ve Çalıştırma
    git clone --recurse-submodules https://github.com/erencsahin/DataProject32Bit.git
    cd DataProject32Bit

- Modülleri derle

    mvn clean package -DskipTests

- Docker ile ayağa kaldır

    docker-compose up -d --build
## Portlar
- tcp -> http://localhost:8081
- rest -> http://localhost:8082/api/rates
- redis -> http://localhost:6379
- opensearchDashboard -> http://localhost:5601
- kafka -> http://localhost:9092
- postgres -> http://localhost:5432
- pgadmin -> http://localhost:5050
## Test

- **Redis** 
    - redis-cli -h localhost -p 6379
    - SELECT 2
    - KEYS avg:*
    - GET avg:USDTRY:2025-06-09T12:00:00.000

- **PostgreSQL** (PgAdmin ile)
    - Tarayıcıdan http://localhost:5050 adresine gidiniz. 
    - username "admin@admin.com" || password "admin"
    - Sol kısımdan **Register** -> **Server**
    - Ekrana çıkan pencerede istediğiniz ismi verin.
    - Connection
        - Hostname: postgres
        - port: 5432
        - maintenance database: toyotadb
        - username: toyota
        - password: secret
    - Şeklinde kaydettikten sonra sol menüden toyotadb server’ına tıklayıp, 
    - Schemas > public > Tables altında tablomuzu görüntüleyebilirsiniz.

- **Kafka**
    - Projemizde terminali açıp.
        - **kafka-console-consumer --bootstrap-server localhost:9092 --topic avg-data --from-beginning**
    - Veriler karşınıza gelecektir.

- **Opensearch Dashboard**
    - http://localhost:5601 adresine gidiniz.
    - Index pattern olarak "**rates**" ve "**toyota-logs-<BulundugumuzTarih**> şeklinde 2 adet pattern çıkacaktır. Ekliyoruz.
    - Soldaki menüden **discover**'a tıklayıp verilerimizi görebiliriz.
