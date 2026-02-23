FROM eclipse-temurin:21-jre

WORKDIR /app
COPY backend/build/libs/backend-all.jar /app/backend.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/backend.jar"]
