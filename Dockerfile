# Ein Image für die ganze Anwendung: Angular-Oberfläche und Spring-Boot-Backend.
# Auf dem Server braucht es dadurch weder Node noch Java, nur Docker.

# --- 1. Oberfläche bauen -----------------------------------------------------------------
FROM node:24-bookworm-slim AS frontend
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npx ng build --output-path=/out

# --- 2. Backend bauen ----------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /src/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
# Abhängigkeiten in einer eigenen Schicht, damit sie bei Codeänderungen im Cache bleiben
RUN ./mvnw -q -B dependency:go-offline
COPY backend/src src
COPY --from=frontend /out/browser/ src/main/resources/static/
# Tests laufen vorher in der Pipeline, hier nur noch paketieren
RUN ./mvnw -q -B package -DskipTests && cp target/test-projekt.jar /app.jar

# --- 3. Laufzeit ----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre
# Eigener Benutzer ohne Root-Rechte
RUN useradd --system --uid 10001 --no-create-home app
COPY --from=backend /app.jar /app/app.jar
USER app

ENV TZ=Europe/Berlin \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
