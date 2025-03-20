# gRPC server builder

FROM --platform=$BUILDPLATFORM gradle:7.6.4-jdk17 AS builder
WORKDIR /build
COPY gradle gradle
COPY gradlew settings.gradle .
RUN sh gradlew wrapper -i
COPY *.gradle .
RUN sh gradlew dependencies -i
COPY . .
RUN sh gradlew build -i

# Extend Override app

FROM amazoncorretto:17-alpine3.21
WORKDIR /app
COPY jars/aws-opentelemetry-agent.jar aws-opentelemetry-agent.jar
COPY --from=builder /build/target/*.jar app.jar
# gRPC server port and and /metrics HTTP port
EXPOSE 6565 8080
CMD [ "java", "-javaagent:aws-opentelemetry-agent.jar", "-jar", "app.jar" ]
