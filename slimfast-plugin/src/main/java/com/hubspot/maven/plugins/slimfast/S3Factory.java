package com.hubspot.maven.plugins.slimfast;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

public class S3Factory {

  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  private S3Factory() {
    throw new AssertionError();
  }

  public static S3AsyncClient createS3AsyncClient(String accessKey, String secretKey) {
    return S3AsyncClient
      .crtBuilder()
      .credentialsProvider(
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
      )
      .region(Region.US_EAST_1)
      .targetThroughputInGbps(20.0)
      // .minimumPartSizeInBytes(8*1025*1024L) default 8mb
      .build();
  }

  public static S3TransferManager createTransferManager(S3AsyncClient s3AsyncClient) {
    return S3TransferManager.builder().s3Client(s3AsyncClient).build();
  }
}
