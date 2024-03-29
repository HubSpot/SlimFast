package com.hubspot.maven.plugins.slimfast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class S3Factory {

  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  private S3Factory() {
    throw new AssertionError();
  }

  public static AmazonS3 create(String accessKey, String secretKey) {
    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    return AmazonS3ClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withClientConfiguration(
        new ClientConfiguration()
          .withConnectionTimeout(2_000)
          .withRequestTimeout(5_000)
          .withMaxErrorRetry(5)
      )
      .build();
  }
}
