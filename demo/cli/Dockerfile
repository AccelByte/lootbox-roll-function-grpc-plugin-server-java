FROM azul/zulu-openjdk:17.0.4-17.36.13 as builder
ARG DEMO_PATH=.
WORKDIR /build
COPY $DEMO_PATH/gradle* ./
COPY $DEMO_PATH/*.gradle ./
COPY $DEMO_PATH/gradle gradle
RUN sh gradlew wrapper -i
COPY $DEMO_PATH/app/*.gradle ./app/
RUN sh gradlew dependencies -i
COPY $DEMO_PATH/ .
RUN sh gradlew installDist

FROM azul/zulu-openjdk:17.0.4-17.36.13
WORKDIR /app
COPY --from=builder /build/app/target/install/app .
ENTRYPOINT /app/bin/app