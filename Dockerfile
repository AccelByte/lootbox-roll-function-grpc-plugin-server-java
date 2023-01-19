FROM azul/zulu-openjdk:17.0.4-17.36.13 as builder
ARG GRADLE_USER_HOME=.gradle
WORKDIR /build
COPY gradle gradle
COPY gradlew settings.gradle ./
RUN sh gradlew wrapper -i
#COPY .gradle* $GRADLE_USER_HOME
COPY *.gradle ./
RUN sh gradlew dependencies -i
COPY . .
RUN sh gradlew build -i


FROM azul/zulu-openjdk:17.0.4-17.36.13
WORKDIR /app
COPY jars/aws-opentelemetry-agent.jar aws-opentelemetry-agent.jar
COPY --from=builder /build/target/*.jar app.jar
# Plugin arch gRPC server port
EXPOSE 6565
# Prometheus /metrics web server port
EXPOSE 8080
ENTRYPOINT exec java -javaagent:aws-opentelemetry-agent.jar $JAVA_OPTS -jar app.jar
