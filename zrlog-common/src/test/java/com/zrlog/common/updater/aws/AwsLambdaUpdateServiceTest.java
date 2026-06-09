package com.zrlog.common.updater.aws;

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwsLambdaUpdateServiceTest {

    @Test
    public void shouldBuildConfigFromEnvironmentMap() {
        Map<String, String> env = new HashMap<>();
        env.put(AwsLambdaUpdateConfig.ACCESS_KEY_ID_ENV, "env-ak");
        env.put(AwsLambdaUpdateConfig.SECRET_ACCESS_KEY_ENV, "env-sk");
        env.put(AwsLambdaUpdateConfig.SESSION_TOKEN_ENV, "env-token");
        env.put(AwsLambdaUpdateConfig.DEFAULT_REGION_ENV, "us-west-1");
        env.put(AwsLambdaUpdateConfig.FUNCTION_NAME_ENV, "xiaochun-zrlog-com");
        env.put(AwsLambdaUpdateConfig.S3_BUCKET_ENV, "deploy-bucket");

        AwsLambdaUpdateConfig config = AwsLambdaUpdateConfig.fromEnvironment(env);

        Assert.assertEquals("env-ak", config.getCredentials().getAccessKeyId());
        Assert.assertEquals("env-sk", config.getCredentials().getSecretAccessKey());
        Assert.assertEquals("env-token", config.getCredentials().getSessionToken());
        Assert.assertEquals("us-west-1", config.getRegion());
        Assert.assertEquals("xiaochun-zrlog-com", config.getFunctionName());
        Assert.assertEquals("deploy-bucket", config.getS3Bucket());
        Assert.assertTrue(config.missingRequiredFields().isEmpty());
    }

    @Test
    public void shouldReportMissingConfigFields() {
        AwsLambdaUpdateConfig config = AwsLambdaUpdateConfig.fromEnvironment(new HashMap<>());

        Assert.assertEquals(4, config.missingRequiredFields().size());
        Assert.assertTrue(config.missingRequiredFields().contains("AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY"));
        Assert.assertTrue(config.missingRequiredFields().contains("AWS_REGION"));
        Assert.assertTrue(config.missingRequiredFields().contains("AWS_LAMBDA_FUNCTION_NAME"));
        Assert.assertTrue(config.missingRequiredFields().contains("LAMBDA_S3_REPO_NAME"));
    }

    @Test
    public void shouldUpdateWithInjectedClient() throws Exception {
        File file = new File("/home/xiaochun/Download/https://dl.zrlog.com/preview/zrlog-3.4.1-SNAPSHOT-d5167b0-preview-Linux-amd64-faas.zip");
        AwsLambdaUpdateConfig config = new AwsLambdaUpdateConfig(
                new AwsCredentials("", "", null),
                "us-west-1",
                "xiaochun-zrlog-com",
                "zrlog-update-bucket"
        );
        //FakeClient fakeClient = new FakeClient();
        AwsLambdaUpdateClient lambdaUpdateClient = new AwsLambdaUpdateClient(config.getCredentials(), config.getRegion());

        String key = "lambda/xiaochun-zrlog-com/test.zip";
        JsonObject response = new AwsLambdaUpdateService(config, lambdaUpdateClient)
                .update(file, key);
        JsonObject jsonObject = lambdaUpdateClient.updateFunctionCode(config.getFunctionName(), config.getS3Bucket(), key);

        Assert.assertEquals("Successful", response.get("LastUpdateStatus").getAsString());
        /*Assert.assertEquals("put:deploy-bucket/lambda/xiaochun-zrlog-com/test.zip", lambdaUpdateClient.calls.get(0));
        Assert.assertEquals("update:xiaochun-zrlog-com:deploy-bucket/lambda/xiaochun-zrlog-com/test.zip",
                lambdaUpdateClient.calls.get(1));*/
    }

    private static File tempPackageFile(String prefix, String suffix) throws Exception {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write("zip".getBytes());
        }
        return file;
    }

    private static class FakeClient implements AwsLambdaCodeUpdateClient {

        private final List<String> calls = new ArrayList<>();

        @Override
        public void putObject(String bucket, String key, File file) {
            calls.add("put:" + bucket + "/" + key);
        }

        @Override
        public JsonObject updateFunctionCode(String functionName, String bucket, String key) {
            calls.add("update:" + functionName + ":" + bucket + "/" + key);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("LastUpdateStatus", "Successful");
            return jsonObject;
        }
    }
}
