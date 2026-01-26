FROM gradle:8.14.3 AS builder

WORKDIR /app

COPY . .

RUN ./gradlew bootJar

FROM eclipse-temurin:21

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

CMD ["java", "-jar", "app.jar"]