# Copyright The OpenTelemetry Authors
# SPDX-License-Identifier: Apache-2.0


FROM --platform=${BUILDPLATFORM} eclipse-temurin:21-jdk AS builder

WORKDIR /usr/src/app/

COPY ./src/analytics/gradlew* ./src/analytics/settings.gradle* ./src/analytics/build.gradle ./
COPY ./src/analytics/gradle ./gradle

RUN ./gradlew
RUN ./gradlew downloadRepos

COPY ./src/analytics/ ./
RUN ./gradlew installDist

# -----------------------------------------------------------------------------

FROM eclipse-temurin:21-jre

ARG OTEL_JAVA_AGENT_VERSION
WORKDIR /usr/src/app/

COPY --from=builder /usr/src/app/ ./
ADD --chmod=644 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v$OTEL_JAVA_AGENT_VERSION/opentelemetry-javaagent.jar /usr/src/app/opentelemetry-javaagent.jar
ENV JAVA_TOOL_OPTIONS=-javaagent:/usr/src/app/opentelemetry-javaagent.jar

ENTRYPOINT [ "./build/install/opentelemetry-demo-analytics/bin/Analytics" ]
