.PHONY: runLocal test lint format

runLocal:
	./gradlew :services:core-server:shadowJar :agents:device-gateway:jar
	docker-compose -f ops/docker-compose.yml up --build

test:
	./gradlew test

lint:
	./gradlew detekt ktlintCheck

format:
	./gradlew ktlintFormat
