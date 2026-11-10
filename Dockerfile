# Build on the host: mvn clean verify && docker build -t skillswap .
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN groupadd --system skillswap && useradd --system --gid skillswap --home-dir /app --shell /usr/sbin/nologin skillswap
COPY --chown=skillswap:skillswap target/skillswap.jar /app/skillswap.jar
USER skillswap:skillswap
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/skillswap.jar"]
