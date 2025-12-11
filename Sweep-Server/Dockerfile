FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy game-logic module
COPY Sweep-Base/game-logic /app/game-logic

# Copy server files
COPY Sweep-Server/build.gradle Sweep-Server/settings.gradle ./
COPY Sweep-Server/src ./src

RUN gradle build --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
