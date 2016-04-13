package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

public class DownloadConfiguration {
  private final Path cacheDirectory;
  private final String s3AccessKey;
  private final String s3SecretKey;

  public DownloadConfiguration(Path cacheDirectory,
                               String s3AccessKey,
                               String s3SecretKey) {
    this.cacheDirectory = cacheDirectory;
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
  }

  public S3Service newS3Service() {
    AWSCredentials credentials = new AWSCredentials(getS3AccessKey(), getS3SecretKey());
    return new RestS3Service(credentials);
  }

  public Path getCacheDirectory() {
    return cacheDirectory;
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }
}
