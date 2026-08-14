# ---------- BUILD ----------

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests


# ---------- RUNTIME ----------

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# API
COPY --from=build /app/target/my-app-name-1.0-SNAPSHOT.jar app.jar

# Worker
COPY --from=build /app/target/my-app-name-1.0-SNAPSHOT-worker.jar worker.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]