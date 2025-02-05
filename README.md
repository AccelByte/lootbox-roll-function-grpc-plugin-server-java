# lootbox-roll-function-grpc-plugin-server-java

```mermaid
flowchart LR
   subgraph AccelByte Gaming Services
   CL[gRPC Client]
   end
   subgraph Extend Override App
   SV["gRPC Server"]
   end
   CL --- SV
```

`AccelByte Gaming Services` (AGS) features can be customized using 
`Extend Override` apps. An `Extend Override` app is basically a `gRPC server` which 
contains one or more custom functions which can be called by AGS instead of the 
default functions.

## Overview

This repository provides a project template to create an `Extend Override` 
app for `lootbox roll function` written in `Java`. It includes an example of how the
custom functions can be implemented. It also includes the essential 
`gRPC server` authentication and authorization to ensure security. Additionally, 
it comes with built-in instrumentation for observability, ensuring that metrics, 
traces, and logs are available upon deployment.

You can clone this repository to begin developing your own `Extend Override` 
app for `lootbox roll function`. Simply modify this project by implementing
your own logic for the custom functions.

## Prerequisites

1. Windows 11 WSL2 or Linux Ubuntu 22.04 or macOS 14+ with the following tools installed:

   a. Bash

      - On Windows WSL2 or Linux Ubuntu:

         ```
         bash --version

         GNU bash, version 5.1.16(1)-release (x86_64-pc-linux-gnu)
         ...
         ```

      - On macOS:

         ```
         bash --version

         GNU bash, version 3.2.57(1)-release (arm64-apple-darwin23)
         ...
         ```

   b. Make

      - On Windows WSL2 or Linux Ubuntu:

         To install from the Ubuntu repository, run `sudo apt update && sudo apt install make`.

         ```
         make --version

         GNU Make 4.3
         ...
         ```

      - On macOS:

         ```
         make --version

         GNU Make 3.81
         ...
         ```

   c. Docker (Docker Desktop 4.30+/Docker Engine v23.0+)
   
      - On Linux Ubuntu:

         1. To install from the Ubuntu repository, run `sudo apt update && sudo apt install docker.io docker-buildx docker-compose-v2`.
         2. Add your user to the `docker` group: `sudo usermod -aG docker $USER`.
         3. Log out and log back in to allow the changes to take effect.

      - On Windows or macOS:

         Follow Docker's documentation on installing the Docker Desktop on [Windows](https://docs.docker.com/desktop/install/windows-install/) or [macOS](https://docs.docker.com/desktop/install/mac-install/).

         ```
         docker version

         ...
         Server: Docker Desktop
            Engine:
            Version:          24.0.5
         ...
         ```

   d. JDK 17

      - On Linux Ubuntu:

         To install from the Ubuntu repository, run: `sudo apt update && sudo apt install openjdk-17-jdk`.

      - On Windows or macOS:

         Follow Microsoft's documentation on [installing the Microsoft Build of OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/install).

         ```
         java --version

         openjdk 17.0.10 2024-01-16
         ...
         ```

   e. [Postman](https://www.postman.com/)

      - Use the available binary from [Postman](https://www.postman.com/downloads/).

   f. [ngrok](https://ngrok.com/)

      - Follow [ngrok's installation guide](https://ngrok.com/downloads).

   g. [extend-helper-cli](https://github.com/AccelByte/extend-helper-cli)

      - Use the available binary from [extend-helper-cli](https://github.com/AccelByte/extend-helper-cli/releases).

   > :exclamation: In macOS, you may use [Homebrew](https://brew.sh/) to easily install some of the tools above.

2. Access to `AccelByte Gaming Services` environment.

   a. Base URL
   
      - For `Shared Cloud` tier e.g.  https://spaceshooter.prod.gamingservices.accelbyte.io
      - For `Private Cloud` tier e.g.  https://dev.accelbyte.io
      
   b. [Create a Game Namespace](https://docs.accelbyte.io/gaming-services/services/access/reference/namespaces/manage-your-namespaces/) if you don't have one yet. Keep the `Namespace ID`.

   c. [Create an OAuth Client](https://docs.accelbyte.io/gaming-services/services/access/authorization/manage-access-control-for-applications/#create-an-iam-client) with confidential client type. Keep the `Client ID` and `Client Secret`.

## Setup

To be able to run this app, you will need to follow these setup steps.

1. Create a docker compose `.env` file by copying the content of [.env.template](.env.template) file.

   > :warning: **The host OS environment variables have higher precedence compared to `.env` file variables**: If the variables in `.env` file do not seem to take effect properly, check if there are host OS environment variables with the same name. 
   See documentation about [docker compose environment variables precedence](https://docs.docker.com/compose/how-tos/environment-variables/envvars-precedence/) for more details.

2. Fill in the required environment variables in `.env` file as shown below.

   ```
   AB_BASE_URL=https://test.accelbyte.io     # Base URL of AccelByte Gaming Services environment
   AB_CLIENT_ID='xxxxxxxxxx'                 # Client ID from the Prerequisites section
   AB_CLIENT_SECRET='xxxxxxxxxx'             # Client Secret from the Prerequisites section
   AB_NAMESPACE='xxxxxxxxxx'                 # Namespace ID from the Prerequisites section
   PLUGIN_GRPC_SERVER_AUTH_ENABLED=true      # Enable or disable access token validation
   ```

   > :exclamation: **In this app, PLUGIN_GRPC_SERVER_AUTH_ENABLED is `true` by default**: If it is set to `false`, the `gRPC server` can be invoked without an AGS access 
   token. This option is provided for development purpose only. It is 
   recommended to enable `gRPC server` access token validation in production 
   environment.

## Building

To build this app, use the following command.

```shell
make build
```

## Running

To (build and) run this app in a container, use the following command.

```shell
docker compose up --build
```

## Testing

### Test in Local Development Environment

> :warning: **To perform the following, make sure PLUGIN_GRPC_SERVER_AUTH_ENABLED is set to `false`**: Otherwise,
the gRPC request will be rejected by the `gRPC server`.

This app can be tested locally using [postman](https://www.postman.com/).

1. Run this app by using the command below.

   ```shell
   docker compose up --build
   ```

2. Open `postman`, create a new `gRPC request`, and enter `localhost:6565` as server URL.

   > :warning: **If you are running [grpc-plugin-dependencies](https://github.com/AccelByte/grpc-plugin-dependencies) stack alongside this project as mentioned in [Test Observability](#test-observability)**: Use `localhost:10000` instead of `localhost:6565`. This way, the `gRPC server` will be called via `Envoy` service within `grpc-plugin-dependencies` stack instead of directly.

3. Continue by selecting `LootBox/RollLootBoxRewards` method and invoke it with the sample message below.

   ```json
   {
      "userId": "b52a2364226d436285c1b8786bc9cbd1",
      "namespace": "accelbyte",
      "quantity": 10,
      "itemInfo": {
         "itemId": "8a0b8bda28c845f6938cc57540af452e",
         "itemSku": "SKU3170",
         "rewardCount": 2,
         "lootBoxRewards": [
               {
                  "name": "Foods",
                  "type": "REWARD",
                  "weight": 5,
                  "odds": 0,
                  "items": [
                     {
                           "itemId": "8b6016d243264c0f90031600313b8a37",
                           "itemSku": "SKU4650",
                           "count": 5
                     }                 
                  ]
               },
               {
                  "name": "Beverages",
                  "type": "REWARD",
                  "weight": 4,
                  "odds": 0,
                  "items": [
                     {
                           "itemId": "dd81bbc3d9fd413daecfd0d0e53fc095",
                           "itemSku": "SKU1939",
                           "count": 13
                     }            
                  ]
               },
               {
                  "name": "Specials",
                  "type": "REWARD",
                  "weight": 1,
                  "odds": 0,
                  "items": [
                     {
                           "itemId": "3318d5fe505a4891b6b5a70586b294ca",
                           "itemSku": "SKU1739",
                           "count": 21
                     }
                  ]
               }
         ]
      }
   }
   ```

4. If successful, you will see the rolled reward(s) in the response.

   ```json
   {
      "rewards": [
         {
            "itemId": "8b6016d243264c0f90031600313b8a37",
            "itemSku": "SKU4650",
            "count": 5
         },
         ...      
      ]
   }
   ```


### Test with AccelByte Gaming Services

For testing this app which is running locally with AGS, the `gRPC server` 
needs to be exposed to the internet. To do this without requiring public IP, we 
can use something like [ngrok](https://ngrok.com/).

1. Run this app by using command below.

   ```shell
   docker compose up --build
   ```

2. Sign-in/sign-up to [ngrok](https://ngrok.com/) and get your auth token in `ngrok` dashboard.

3. In this app root directory, run the following helper command to expose `gRPC server` port in local development environment to the internet. Take a note of the `ngrok` forwarding URL e.g. `http://0.tcp.ap.ngrok.io:xxxxx`.

   ```
   make ngrok NGROK_AUTHTOKEN=xxxxxxxxxxx
   ```

   > :warning: **If you are running [grpc-plugin-dependencies](https://github.com/AccelByte/grpc-plugin-dependencies) stack alongside this app as mentioned in [Test Observability](#test-observability)**: Run the above 
   command in `grpc-plugin-dependencies` directory instead of this app directory. 
   This way, the `gRPC server` will be called via `Envoy` service within `grpc-plugin-dependencies` stack instead of directly.

4. [Create an OAuth Client](https://docs.accelbyte.io/gaming-services/services/access/authorization/manage-access-control-for-applications/#create-an-iam-client) with `confidential` client type with the following permissions. Keep the `Client ID` and `Client Secret`.
   
   - For AGS Private Cloud customers:
      - `ADMIN:NAMESPACE:{namespace}:PLUGIN:CATALOG [READ,UPDATE,DELETE]`
      - `ADMIN:NAMESPACE:{namespace}:STORE [CREATE,READ,UPDATE,DELETE]`
      - `ADMIN:NAMESPACE:{namespace}:CATEGORY [CREATE]`
      - `ADMIN:NAMESPACE:{namespace}:CURRENCY [CREATE,READ,DELETE]`
      - `ADMIN:NAMESPACE:{namespace}:ITEM [CREATE,READ,DELETE]`
      - `ADMIN:NAMESPACE:{namespace}:USER:*:ENTITLEMENT [CREATE,READ,UPDATE,DELETE]`
   - For AGS Shared Cloud customers:
      - Platform Store -> Service Plugin Config (Read, Update, Delete)
      - Platform Store -> Store (Create, Read, Update, Delete)
      - Platform Store -> Category (Create)
      - Platform Store -> Currency (Create, Read, Delete)
      - Platform Store -> Item (Create, Read, Delete)
      - Platform Store -> Entitlement (Create, Read, Update, Delete)

   > :warning: **Oauth Client created in this step is different from the one from Prerequisites section:** It is required by CLI demo app [here](demo/cli/) in the next step to register the `gRPC Server` URL.

5. Create a user for testing. Keep the `Username` and `Password`.

6. In [demo/cli](demo/cli) folder, create an `.env` file by copying the content of [.env.template](demo/cli/.env.template) file and
set the required environment variables as shown below. 

   ```
   AB_BASE_URL='https://test.accelbyte.io'
   AB_CLIENT_ID='xxxxxxxxxx'       # Use Client ID from the previous step
   AB_CLIENT_SECRET='xxxxxxxxxx'   # Use Client secret from the previous step
   AB_NAMESPACE='xxxxxxxxxx'       # Use your Namespace ID
   AB_USERNAME='xxxxxxxxxx'        # Use your Namespace Username
   AB_PASSWORD='xxxxxxxxxx'        # Use your Namespace Password
   GRPC_SERVER_URL='0.tcp.ap.ngrok.io:xxxxx'   # Use your ngrok forwarding URL without `https://`
   ```

   Run the [Makefile](demo/cli/Makefile) commands to execute the CLI demo app.

   ```
   $ cd demo/cli
   $ make build
   $ make run-only ENV_FILE_PATH=.env
   ```

> :warning: **Ngrok free plan has some limitations**: You may want to use paid plan if the traffic is high.

### Test Observability

To be able to see the how the observability works in this app locally, there are few things that need be setup before performing tests.

1. Uncomment loki logging driver in [docker-compose.yaml](docker-compose.yaml)

   ```
    # logging:
    #   driver: loki
    #   options:
    #     loki-url: http://host.docker.internal:3100/loki/api/v1/push
    #     mode: non-blocking
    #     max-buffer-size: 4m
    #     loki-retries: "3"
   ```

   > :warning: **Make sure to install docker loki plugin beforehand**: Otherwise,
   this project will not be able to run. This is required so that container logs
   can flow to the `loki` service within `grpc-plugin-dependencies` stack. 
   Use this command to install docker loki plugin: `docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions`.

2. Clone and run [grpc-plugin-dependencies](https://github.com/AccelByte/grpc-plugin-dependencies) stack alongside this project. After this, Grafana 
will be accessible at http://localhost:3000.

   ```
   git clone https://github.com/AccelByte/grpc-plugin-dependencies.git
   cd grpc-plugin-dependencies
   docker-compose up
   ```

   > :exclamation: More information about [grpc-plugin-dependencies](https://github.com/AccelByte/grpc-plugin-dependencies) is available [here](https://github.com/AccelByte/grpc-plugin-dependencies/blob/main/README.md).

3. Perform testing. For example, by following [Test in Local Development Environment](#test-in-local-development-environment) or [Test with AccelByte Gaming Services](#test-with-accelbyte-gaming-services).

## Deploying

After done testing, you may want to deploy this app to `AccelByte Gaming Services`.

1. [Create a new Extend Override App on Admin Portal](https://docs.accelbyte.io/gaming-services/services/extend/override/getting-started-with-lootbox-roll-customization/#create-the-extend-app). Keep the `Repository URI`.

2. Download and setup [extend-helper-cli](https://github.com/AccelByte/extend-helper-cli/) (only if it has not been done previously).

3. Perform docker login with `extend-helper-cli` using the following command.

   ```
   extend-helper-cli dockerlogin --namespace <my-game> --app <my-app> --login
   ```

   > :exclamation: For your convenience, the above `extend-helper-cli` command can also be 
   copied from `Repository Authentication Command` under the corresponding app detail page.

4. Build and push this project docker image to AccelByte ECR using the following command.
   
   ```
   extend-helper-cli image-upload --work-dir <my-project-dir> --namespace <my-game> --app <my-app> --image-tag v0.0.1
   ```

   > :warning: Make sure to perform docker login (step 3) before executing the above command.

5. Open Admin Portal, go to **Extend** -> **Overridable Features**. And then select the extend app.

6. To deploy selected image tag, click **Image Version History** and select 
   desired image tag to be deployed.

7. Click **Deploy Image**, confirm the deployment and go back to App Detail by 
   clicking **Cancel**.

8. Wait until app status is running.

## Next Step

Proceed to create your own `Extend Override` app for `lootbox roll function` by modifying this project. See [here](https://docs.accelbyte.io/gaming-services/services/extend/override/lootbox-roll-function/) for more details.