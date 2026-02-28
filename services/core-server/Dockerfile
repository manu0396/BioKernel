FROM eclipse-temurin:21-jre

WORKDIR /app
COPY services/core-server/build/libs/core-server.jar /app/core-server.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/core-server.jar"]
