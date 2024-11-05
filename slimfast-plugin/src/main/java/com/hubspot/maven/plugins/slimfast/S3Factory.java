package com.hubspot.maven.plugins.slimfast;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

public class S3Factory {

  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  private S3Factory() {
    throw new AssertionError();
  }

  public static S3AsyncClient createS3AsyncClient(S3Configuration config) {
    AwsCredentialsProvider credentialsProvider = null;
    if (config.getAccessKey().isPresent() && config.getSecretKey().isPresent()) {
      credentialsProvider =
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create(
            config.getAccessKey().get(),
            config.getSecretKey().get()
          )
        );
    }

    S3CrtAsyncClientBuilder clientBuilder = S3AsyncClient
      .crtBuilder()
      .credentialsProvider(credentialsProvider);

    config.getRegion().ifPresent(clientBuilder::region);
    config.getTargetThroughputGbps().ifPresent(clientBuilder::targetThroughputInGbps);
    config.getMinPartSizeBytes().ifPresent(clientBuilder::minimumPartSizeInBytes);

    return clientBuilder.build();
  }

  public static S3TransferManager createTransferManager(S3AsyncClient s3AsyncClient) {
    return S3TransferManager.builder().s3Client(s3AsyncClient).build();
  }
}
