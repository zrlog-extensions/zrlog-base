package com.zrlog.common.updater.aws;

import com.google.gson.JsonObject;
import com.hibegin.common.util.StringUtils;

import java.io.File;
import java.util.Objects;

public class AwsLambdaUpdateService {

    private final AwsLambdaUpdateConfig config;
    private final AwsLambdaCodeUpdateClient client;

    public AwsLambdaUpdateService(AwsLambdaUpdateConfig config) {
        this(config, new AwsLambdaUpdateClient(config.getCredentials(), config.getRegion()));
    }

    public AwsLambdaUpdateService(AwsLambdaUpdateConfig config, AwsLambdaCodeUpdateClient client) {
        this.config = config;
        this.client = client;
    }

    public JsonObject update(File packageFile, String s3Key) throws Exception {
        validate(config, packageFile, s3Key);
        client.putObject(config.getS3Bucket(), s3Key, packageFile);
        return client.updateFunctionCode(config.getFunctionName(), config.getS3Bucket(), s3Key);
    }

    private static void validate(AwsLambdaUpdateConfig config, File packageFile, String s3Key) {
        config.validate();
        if (StringUtils.isEmpty(s3Key)) {
            throw new IllegalArgumentException("Missing S3 object key");
        }
        if (Objects.isNull(packageFile) || !packageFile.exists() || !packageFile.isFile()) {
            throw new IllegalArgumentException("Missing package file " + packageFile);
        }
        if (packageFile.length() <= 0) {
            throw new IllegalArgumentException("Empty package file " + packageFile);
        }
    }
}
