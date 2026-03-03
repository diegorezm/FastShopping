# ---- Build stage ----
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copy gradle/maven files first for layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---- Run stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy jar from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# JVM flags optimized for containers
ENV JAVA_OPTS="\
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -Xms512m \
  -Xmx1536m \
  -XX:MaxMetaspaceSize=256m \
  -XX:+AlwaysPreTouch \
  -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]