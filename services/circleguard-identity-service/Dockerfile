FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S circleguard && adduser -S circleguard -G circleguard
USER circleguard:circleguard

ARG JAR_FILE=build/libs/*-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]