FROM --platform=$BUILDPLATFORM gradle:7.6.4-jdk17 AS builder
ARG GRADLE_USER_HOME=.gradle
WORKDIR /build
COPY gradle gradle
COPY *.gradle ./
RUN gradle dependencies -i
COPY . .
RUN gradle build -i


FROM ibm-semeru-runtimes:open-17-jre
WORKDIR /app
COPY jars/aws-opentelemetry-agent.jar aws-opentelemetry-agent.jar
COPY --from=builder /build/target/*.jar app.jar
# gRPC server port, Prometheus /metrics http port
EXPOSE 6565 8080
ENTRYPOINT exec java -javaagent:aws-opentelemetry-agent.jar $JAVA_OPTS -jar app.jar
