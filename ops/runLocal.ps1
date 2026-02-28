Param()

./gradlew.bat :services:core-server:shadowJar :agents:device-gateway:jar

docker-compose -f ops/docker-compose.yml up --build
