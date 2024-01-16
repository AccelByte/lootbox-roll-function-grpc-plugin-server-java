FROM ngrok/ngrok as ngrok_image

FROM gradle:7.5.1-jdk17-alpine
WORKDIR /workspace
RUN apk --no-cache add bash curl docker-cli-buildx git jq make gcompat \
        && apk add --update-cache --upgrade curl
COPY --from=ngrok_image /bin/ngrok /bin/ngrok
