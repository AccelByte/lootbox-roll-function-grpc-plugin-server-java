# Platform Service's Custom Lootbox Roll Plugin gRPC Demo App

A CLI demo app to prepare required data and execute Custom Lootbox Roll Plugin gRPC for AGS's Platform Service.
Following diagram will explain how this CLI demo app works.
```mermaid
sequenceDiagram
    participant A as CLI Demo App
    participant I as AGS IAM
    participant P as AGS Platform
    participant G as Grpc Plugin Server
    
    A ->> I: user login
    I -->> A: auth token
    A ->> P: Set gRpc server target
    A ->> P: Create draft store and publish it
    A ->> P: Create item's category    
    A ->> P: Create lootbox item(s)
    A ->> P: Grant item entitlement to user
    A ->> P: Consume entitlement    
    P ->> G: Call lootbox roll function
    G -->> P: Returns rewards
    P -->> A: Return rewards
    A ->> P: Delete store
    A ->> P: Unset gRpc server target
```

## Prerequsites

* Java 11
* Gradle

## Build

To build this CLI sample app, execute the following command.

```bash
$ ./gradlew installDist
```
or
```bash
$ ./gradlew imageNative
```
to utilize GraalVm.

## Usage

### Setup

The following environment variables are used by this CLI demo app.
```
export AB_BASE_URL='https://demo.accelbyte.io'
export AB_CLIENT_ID='xxxxxxxxxx'
export AB_CLIENT_SECRET='xxxxxxxxxx'

export AB_NAMESPACE='namespace'
export AB_USERNAME='USERNAME'
export AB_PASSWORD='PASSWORD'
```
If these variables aren't provided, you'll need to supply the required values via command line arguments.

Also, you will need `Custom Lootbox Roll Plugin gRPC` server already deployed and accessible. If you want to use your local development environment, you can use tunneling service like `ngrok` to tunnel your grpc server port so it can be accessed by AGS.
> Current AGS deployment does not support mTLS and authorization for custom grpc plugin. Make sure you disable mTls and authorization in your deployed Grpc server.

### Executable
- Without GraalVm, the executable is:
```
app/target/install/app/bin/app
```
- Or, with GraalVm, the executable is:
```
app/target/graal/platformGrpcDemo
```

### Example
CLI demo app requires only the grpc server url as a parameter.

- Without any environment variables
```bash
$ ./app/target/install/app/bin/app -b='https://demo.accelbyte.io' -c='CLIENT-ID-VALUE' -s='CLIENT-SECRET-VALUE' -n='NAMESPACE-VALUE' -u='<USERNAME>' -p='<PASSWORD>' -g='<GRPC_PLUGIN_SERVER_URL>'
```
- With basic environment variables setup
```bash
$ ./app/target/install/app/bin/app -u='<USERNAME>' -p='<PASSWORD>' -n='<NAMESPACE-VALUE>' -g='<GRPC_PLUGIN_SERVER_URL>'
```
- With all environment variables setup
```bash
$ ./app/target/install/app/bin/app -g='<GRPC_PLUGIN_SERVER_URL>'
```
- Use Extend App instead of grpc server url
```bash
$ ./app/target/install/app/bin/app -a='<EXTEND_APP_NAME>'
```

- Show usage help
```bash
 $ ./app/target/install/app/bin/app -h
```