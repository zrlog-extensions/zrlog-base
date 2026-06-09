package com.zrlog.common.updater.aws;

import com.hibegin.common.util.StringUtils;

public class AwsCredentials {

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;

    public AwsCredentials(String accessKeyId, String secretAccessKey, String sessionToken) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = sessionToken;
    }

    public static AwsCredentials fromEnvironment() {
        return new AwsCredentials(
                System.getenv("AWS_ACCESS_KEY_ID"),
                System.getenv("AWS_SECRET_ACCESS_KEY"),
                System.getenv("AWS_SESSION_TOKEN")
        );
    }

    public boolean isValid() {
        return StringUtils.isNotEmpty(accessKeyId) && StringUtils.isNotEmpty(secretAccessKey);
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
