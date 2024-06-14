#!/usr/bin/env bash

# Prerequisites: bash, curl, go, jq, ngrok

set -e
set -o pipefail
#set -x

function clean_up()
{
  kill -9 $GRPC_SERVER_PID $NGROK_PID
}

trap clean_up EXIT

echo '# Build and run Extend app locally'

bash gradlew -i --no-daemon generateProto \
    || find $GRADLE_USER_HOME -type f -iname 'protoc-*.exe' -exec chmod +x {} \;   # For MacOS docker host: Workaround to make protoc-*.exe executable
bash gradlew -i --no-daemon clean build
bash gradlew -i --no-daemon run & GRPC_SERVER_PID=$!

(for _ in {1..12}; do bash -c "timeout 1 echo > /dev/tcp/127.0.0.1/8080" 2>/dev/null && exit 0 || sleep 5s; done; exit 1)

if [ $? -ne 0 ]; then
  echo "Failed to run Extend app locally"
  exit 1
fi

echo '# Check environment variables'

variables=(AB_BASE_URL AB_CLIENT_ID AB_CLIENT_SECRET AB_NAMESPACE AB_USERNAME AB_PASSWORD NGROK_AUTHTOKEN)

for variable_name in "${variables[@]}"; do
  if [ -z "${!variable_name}" ]; then
    echo "Variable $variable_name is empty"
    exit 1
  fi
done

echo '# Run ngrok'

( ngrok tcp 6565 > ngrok.log 2>&1 ) & NGROK_PID=$!

for _ in {1..12}; do
  sleep 5
  NGROK_RESPONSE=$(curl -s --location 'localhost:4040/api/tunnels')
  NGROK_URL=$(echo "$NGROK_RESPONSE" | jq -r '.tunnels[] | select(.config.addr = "localhost:6565") | .public_url')
  if [ -n "$NGROK_URL" ]; then
      break
  fi
done

if [ -z "$NGROK_URL" ]; then
  echo "Failed to run ngrok"
  exit 1
fi

if [ -z "$GRPC_SERVER_URL" ]; then
    echo "GRPC_SERVER_URL is not set. Setting it now..."
    export GRPC_SERVER_URL="${NGROK_URL#*://}"
fi

echo '# Testing Extend app using demo CLI'

GRPC_SERVER_URL="http://${NGROK_URL#*://}"

cd demo/cli && bash gradlew installDist && ./app/target/install/app/bin/app -g="${NGROK_URL#*://}"

