FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY . .
RUN mvn clean package -pl coordinator -am -DskipTests -q

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=builder /build/coordinator/target/coordinator-*.jar app.jar
EXPOSE 1502
ENTRYPOINT ["java","-jar","app.jar"]