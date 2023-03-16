/*
 * Copyright (c) 2023 AccelByte Inc. All Rights Reserved
 * This is licensed software from AccelByte Inc, for limitations
 * and restrictions contact your company contract manager.
 */
package net.accelbyte.extend.platform.demo;

import picocli.CommandLine;

public class App {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PlatformDemo()).execute(args);
        System.exit(exitCode);
    }
}
