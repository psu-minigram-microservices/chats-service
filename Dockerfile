FROM azul/zulu-openjdk:25 AS build

WORKDIR /app

COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew && \
    --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon || true

COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon -x test

FROM azul/zulu-openjdk:25-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
