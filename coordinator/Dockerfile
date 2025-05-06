# 1) Build aşaması
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Sadece ihtiyaç duyduklarımızı kopyala
COPY pom.xml .
COPY src ./src

# mvnw yerine doğrudan mvn kullanıyoruz:
RUN mvn clean package -DskipTests

# 2) Run aşaması
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Oluşan jar'ı al
COPY --from=builder /build/target/coordinator-*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
