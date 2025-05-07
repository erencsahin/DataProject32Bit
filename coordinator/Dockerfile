FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 1. Parent POM'u kopyala
COPY pom.xml .

# 2. Tüm modül dizinlerini kopyala
COPY rest/pom.xml ./rest/
COPY tcp/pom.xml ./tcp/
COPY coordinator/pom.xml ./coordinator/

# 3. Sadece ilgili modülün kaynak kodunu kopyala
COPY coordinator/src ./coordinator/src

RUN mvn clean package -pl coordinator -am -DskipTests -q

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=builder /app/coordinator/target/coordinator-*.jar app.jar
EXPOSE 1502
ENTRYPOINT ["java","-jar","app.jar"]