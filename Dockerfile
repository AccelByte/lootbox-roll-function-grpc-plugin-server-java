FROM --platform=$BUILDPLATFORM gradle:7.6.4-jdk17 AS builder
WORKDIR /build
COPY gradle gradle
COPY gradlew settings.gradle .
RUN sh gradlew wrapper -i
COPY *.gradle .
RUN sh gradlew dependencies -i
COPY . .
RUN sh gradlew build -i


FROM alpine:3.18
RUN apk add --no-cache openjdk17
WORKDIR /app
COPY jars/aws-opentelemetry-agent.jar aws-opentelemetry-agent.jar
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 6565 8080
ENTRYPOINT exec java -javaagent:aws-opentelemetry-agent.jar $JAVA_OPTS -jar app.jar
