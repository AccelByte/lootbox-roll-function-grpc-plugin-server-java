/*
 * Copyright (c) 2023 AccelByte Inc. All Rights Reserved
 * This is licensed software from AccelByte Inc, for limitations
 * and restrictions contact your company contract manager.
 */
package net.accelbyte.extend.platform.demo;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import net.accelbyte.sdk.core.repository.DefaultConfigRepository;

public class AppConfigRepository extends DefaultConfigRepository {

    @Option(names = {"-b","--baseurl"}, description = "AGS base URL", defaultValue = "")
    private String abBaseUrl;

    @Option(names = {"-c","--client"}, description = "AGS client id", defaultValue = "")
    private String abClientId;

    @Option(names = {"-s","--secret"}, description = "AGS client secret", defaultValue = "")
    private String abClientSecret;

    @Option(names = {"-n","--namespace"}, description = "AGS namespace", defaultValue = "")
    private String abNamespace;

    @Option(names = {"-u","--username"}, description = "AGS Username", defaultValue = "")
    private String abUsername;

    @Option(names = {"-p","--password"}, description = "AGS User's password", defaultValue = "")
    private String abPassword;

    @Option(names = {"-t","--category"}, description = "Store's category path for items", defaultValue = "")
    private String platformCategoryPath;

    @Parameters(index = "0", paramLabel = "GRPC_TARGET", description = "Grpc plugin target server url.")
    private String grpcServerUrl;

    @Override
    public String getClientId() {
        if (!abClientId.equals(""))
            return abClientId;
        else
            return super.getClientId();
    }

    @Override
    public String getClientSecret() {
        if (!abClientSecret.equals(""))
            return abClientSecret;
        else
            return super.getClientSecret();
    }

    @Override
    public String getBaseURL() {
        if (!abBaseUrl.equals(""))
            return abBaseUrl;
        else
            return super.getBaseURL();
    }

    public String getNamespace() throws Exception {
        if (abNamespace.equals("")) {
            abNamespace = System.getenv("AB_NAMESPACE");
            if (abNamespace == null) {
                throw new Exception("Namespace is required.");
            }
        }
        return abNamespace;
    }

    public String getGrpcServerUrl() {
        if (grpcServerUrl.equals("")) {
            grpcServerUrl = System.getenv("AB_GRPC_SERVER_URL");
            if (grpcServerUrl == null)
                grpcServerUrl = "";
        }
        return grpcServerUrl;
    }

    public String getUsername() throws Exception {
        if (abUsername.equals("")) {
            abUsername = System.getenv("AB_USERNAME");
            if (abUsername == null) {
                throw new Exception("Username is not specified.");
            }
        }
        return abUsername;
    }

    public String getPassword() throws Exception {
        if (abPassword.equals("")) {
            abPassword = System.getenv("AB_PASSWORD");
            if (abPassword == null) {
                throw new Exception("User's password is not specified.");
            }
        }
        return abPassword;
    }

    public String getCategoryPath(){
        if (platformCategoryPath.equals(""))
            return "/customitemrotationtest";
        else
            return  "/" + platformCategoryPath.trim();
    }
}
