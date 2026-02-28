FROM eclipse-temurin:21-jre

WORKDIR /app
COPY agents/device-gateway/build/libs/device-gateway.jar /app/device-gateway.jar

CMD ["java", "-jar", "/app/device-gateway.jar"]
