#!/usr/bin/env bash

# Prerequisites: bash, curl, go, jq

set -e
set -o pipefail
#set -x

APP_NAME=int-test-lootbox

get_code_verifier() 
{
  echo $RANDOM | sha256sum | cut -d ' ' -f 1   # For testing only: In reality, it needs to be secure random
}

get_code_challenge()
{
  echo -n "$1" | sha256sum | xxd -r -p | base64 | tr -d '\n' | sed -e 's/+/-/g' -e 's/\//\_/g' -e 's/=//g'
}

function api_curl()
{
  HTTP_CODE=$(curl -s -o response.out -w '%{http_code}' "$@")
  cat response.out
  echo
  echo $HTTP_CODE | grep -q '\(200\|201\|204\|302\)' || exit 123
}

function clean_up()
{
    echo '# Delete Extend app'
    
    api_curl "${AB_BASE_URL}/csm/v1/admin/namespaces/$AB_NAMESPACE/apps/$APP_NAME" \
        -X 'DELETE' \
        -H "Authorization: Bearer $ACCESS_TOKEN"

    echo '# Delete OAuth client'

    OAUTH_CLIENT_LIST=$(api_curl "${AB_BASE_URL}/iam/v3/admin/namespaces/$AB_NAMESPACE/clients?clientName=extend-$APP_NAME&limit=20" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    OAUTH_CLIENT_LIST_COUNT=$(echo "$OAUTH_CLIENT_LIST" | jq '.data | length')

    if [ "$OAUTH_CLIENT_LIST_COUNT" -eq 0 ] || [ "$OAUTH_CLIENT_LIST_COUNT" -gt 1 ]; then
      echo "Failed to to clean up OAuth client (name: extend-$APP_NAME)"
      exit 1
    fi

    OAUTH_CLIENT_ID="$(echo "$OAUTH_CLIENT_LIST" | jq -r '.data[0].clientId')"

    api_curl "${AB_BASE_URL}/iam/v3/admin/namespaces/$AB_NAMESPACE/clients/$OAUTH_CLIENT_ID" \
        -X 'DELETE' \
        -H "Authorization: Bearer $ACCESS_TOKEN"
}

APP_NAME="${APP_NAME}-$(echo $RANDOM | sha1sum | head -c 8)"   # Add random suffix to make it easy to clean up

echo '# Downloading extend-helper-cli'

curl -sf https://api.github.com/repos/AccelByte/extend-helper-cli/releases/latest \
        | grep "/extend-helper-cli-linux" | cut -d : -f 2,3 | tr -d \" \
        | curl -sL --output extend-helper-cli $(cat)
chmod +x ./extend-helper-cli

echo '# Check environment variables'

variables=(AB_BASE_URL AB_CLIENT_ID AB_CLIENT_SECRET AB_NAMESPACE AB_USERNAME AB_PASSWORD NGROK_AUTHTOKEN)

for variable_name in "${variables[@]}"; do
  if [ -z "${!variable_name}" ]; then
    echo "Variable $variable_name is empty"
    exit 1
  fi
done

echo '# Login user'

CODE_VERIFIER="$(get_code_verifier)"
CODE_CHALLENGE="$(get_code_challenge "$CODE_VERIFIER")"
REQUEST_ID="$(curl -sf -D - "${AB_BASE_URL}/iam/v3/oauth/authorize?scope=commerce+account+social+publishing+analytics&response_type=code&code_challenge_method=S256&code_challenge=$CODE_CHALLENGE&client_id=$AB_CLIENT_ID" | grep -o 'request_id=[a-f0-9]\+' | cut -d= -f2)"
CODE="$(curl -sf -D - ${AB_BASE_URL}/iam/v3/authenticate -H 'Content-Type: application/x-www-form-urlencoded' -d "password=$AB_PASSWORD&user_name=$AB_USERNAME&request_id=$REQUEST_ID&client_id=$AB_CLIENT_ID" | grep -o 'code=[a-f0-9]\+' | cut -d= -f2)"
ACCESS_TOKEN="$(curl -sf ${AB_BASE_URL}/iam/v3/oauth/token -H 'Content-Type: application/x-www-form-urlencoded' -u "$AB_CLIENT_ID:$AB_CLIENT_SECRET" -d "code=$CODE&grant_type=authorization_code&client_id=$AB_CLIENT_ID&code_verifier=$CODE_VERIFIER" | jq --raw-output .access_token)"

echo '# Create Extend app'

api_curl "${AB_BASE_URL}/csm/v1/admin/namespaces/${AB_NAMESPACE}/apps/$APP_NAME" \
  -X 'PUT' \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'content-type: application/json' \
  --data-raw '{"scenario":"function-override","description":"Extend integration test"}'

trap clean_up EXIT

for _ in {1..60}; do
    STATUS=$(api_curl "${AB_BASE_URL}/csm/v1/admin/namespaces/${AB_NAMESPACE}/apps?limit=500&offset=0" \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            -H 'content-type: application/json' \
            --data-raw "{\"appNames\":[\"${APP_NAME}\"],\"statuses\":[],\"scenario\":\"function-override\"}" \
            | jq -r '.data[0].status')
    if [ "$STATUS" = "S" ]; then
        break
    fi
    echo "Waiting until Extend app created (status: $STATUS)"
    sleep 10
done

if ! [ "$STATUS" = "S" ]; then
    echo "Failed to create Extend app (status: $STATUS)"
    exit 1
fi

echo '# Build and push Extend app'

APP_DETAILS=$(api_curl "${AB_BASE_URL}/csm/v1/admin/namespaces/${AB_NAMESPACE}/apps/$APP_NAME" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

APP_REPO_URL=$(echo "$APP_DETAILS" | jq -r '.appRepoUrl')
APP_REPO_HOST=$(echo "$APP_REPO_URL" | cut -d/ -f1)

./extend-helper-cli dockerlogin --namespace $AB_NAMESPACE --app $APP_NAME -p | docker login -u AWS --password-stdin $APP_REPO_HOST

#make build
make imagex_push REPO_URL=$APP_REPO_URL IMAGE_TAG=v0.0.1

api_curl "${AB_BASE_URL}/csm/v1/admin/namespaces/${AB_NAMESPACE}/apps/$APP_NAME/deployments" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'content-type: application/json' \
  --data-raw '{"imageTag":"v0.0.1","description":""}' \

for _ in {1..60}; do
    STATUS=$(api_curl "${AB_BASE_URL}/csm/v1/admin/namespaces/${AB_NAMESPACE}/apps?limit=500&offset=0" \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            -H 'content-type: application/json' \
            --data-raw "{\"appNames\":[\"${APP_NAME}\"],\"statuses\":[],\"scenario\":\"function-override\"}" \
            | jq -r '.data[0].app_release_status')
    if [ "$STATUS" = "R" ]; then
        break
    fi
    echo "Waiting until Extend app deployed (status: $STATUS)"
    sleep 10
done

if ! [ "$STATUS" = "R" ]; then
    echo "Failed to deploy Extend app (status: $STATUS)"
    exit 1
fi

echo '# Testing Extend app using demo CLI'

cd demo/cli && bash gradlew installDist && ./app/target/install/app/bin/app -a="$APP_NAME"
