# syntax=docker/dockerfile:1.7
#
# Runtime-only image: the jar is built beforehand by CI (`mvn verify`), not
# inside this Dockerfile - same pattern as every other service on this
# platform. Replaces the previous single-stage `FROM openjdk:17` image
# (deprecated base, root-run, no healthcheck, whole JDK baked in).

FROM gcr.io/distroless/java21-debian12:nonroot AS builder

WORKDIR /app
COPY target/config-server.jar /app/app.jar

RUN ["java", "-Djarmode=tools", "-jar", "/app/app.jar", "extract", "--layers", "--launcher", "--destination", "/app/extracted"]

FROM gcr.io/distroless/java21-debian12:nonroot

ARG VCS_REF=unknown
ARG BUILD_DATE=unknown
ARG VERSION=unknown

LABEL org.opencontainers.image.title="config-server" \
      org.opencontainers.image.description="Git-backed centralized config server for the IoTMining/IIoTEdge platform" \
      org.opencontainers.image.source="https://github.com/IotMining/config-server" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.licenses="MIT"

WORKDIR /app
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC -Dfile.encoding=UTF-8 -Duser.timezone=UTC -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8888

EXPOSE 8888

# No Docker-level HEALTHCHECK: distroless has no shell/curl/wget to run one
# from - /actuator/health (unauthenticated, see SecurityConfig) is meant
# to be probed by the orchestrator instead.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
