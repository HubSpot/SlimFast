package com.hubspot.maven.plugins.slimfast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.StringUtils;

public class S3Factory {

  private S3Factory() {
    throw new AssertionError();
  }

  public static AmazonS3 create(String accessKey, String secretKey, String endpoint) {
    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    AmazonS3ClientBuilder amazonS3ClientBuilder = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withClientConfiguration(
                    new ClientConfiguration()
                            .withConnectionTimeout(2_000)
                            .withRequestTimeout(5_000)
                            .withMaxErrorRetry(5)
            );

    if (!StringUtils.isNullOrEmpty(endpoint)) {
      AwsClientBuilder.EndpointConfiguration enpointConfig = new AwsClientBuilder.EndpointConfiguration(endpoint, "");
      amazonS3ClientBuilder.withEndpointConfiguration(enpointConfig);
    }
    return amazonS3ClientBuilder.build();
  }
}
