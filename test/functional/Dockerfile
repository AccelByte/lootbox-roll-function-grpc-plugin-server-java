FROM alpine:3.19
WORKDIR /workspace
RUN apk add --no-cache \
        bash \
        curl \
        docker-cli-buildx \
        gcompat \
        git \
        jq \
        libstdc++ \
        make \
        openjdk17-jdk
COPY --from=ngrok/ngrok:latest /bin/ngrok /bin/ngrok