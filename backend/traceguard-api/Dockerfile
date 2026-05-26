FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S traceguard && adduser -S traceguard -G traceguard \
  && apk add --no-cache wget \
  && mkdir -p /app/logs \
  && chown -R traceguard:traceguard /app

COPY --from=builder /workspace/target/*.jar /app/app.jar

USER traceguard
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]
