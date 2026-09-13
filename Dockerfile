FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies

COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install --yes --no-install-recommends tesseract-ocr tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --uid 10001 appuser
WORKDIR /app
COPY --from=build --chown=appuser:appuser /workspace/build/libs/*.jar app.jar
RUN mkdir -p /data && chown appuser:appuser /data
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
