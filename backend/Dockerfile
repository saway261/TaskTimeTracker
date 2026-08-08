# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src

# Tests are expected to run in CI; the production image only needs the executable WAR.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootWar --no-daemon -x test && \
    cp build/libs/*.war /workspace/app.war


FROM eclipse-temurin:25-jre-noble AS runtime

WORKDIR /app

COPY --from=builder --chown=10001:10001 /workspace/app.war ./app.war

ENV PORT=8080
EXPOSE 8080

USER 10001:10001

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -Dserver.port=${PORT} -jar /app/app.war"]
