# Analytics Service

The Analytics service counts the number of ads being send to customers and in which category the were requested

## Building Locally

The Ad service requires at least JDK 17 to build and uses gradlew to
compile/install/distribute. Gradle wrapper is already part of the source code.
To build Ad Service, run:

```sh
./gradlew installDist
```

It will create an executable script
`src/analytics/build/install/oteldemo/bin/Analytics`.

To run the Ad Service:

```sh
export ANALYTICS_SERVICE_PORT=8080
export FEATURE_FLAG_GRPC_SERVICE_ADDR=featureflagservice:50053
./build/install/opentelemetry-demo-analytics/bin/AdService
```

### Upgrading Gradle

If you need to upgrade the version of gradle then run

```sh
./gradlew wrapper --gradle-version <new-version>
```

## Building Docker

From the root of `opentelemetry-demo`, run:

```sh
docker build --file ./src/analytics/Dockerfile ./
```
