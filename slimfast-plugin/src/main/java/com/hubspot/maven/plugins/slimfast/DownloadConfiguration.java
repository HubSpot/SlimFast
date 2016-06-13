package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

public class DownloadConfiguration {
  private final Path prefix;
  private final Path cacheDirectory;
  private final Path outputDirectory;
  private final String s3AccessKey;
  private final String s3SecretKey;

  public DownloadConfiguration(Path prefix,
                               Path cacheDirectory,
                               Path outputDirectory,
                               String s3AccessKey,
                               String s3SecretKey) {
    this.prefix = prefix;
    this.cacheDirectory = cacheDirectory;
    this.outputDirectory = outputDirectory;
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
  }

  public S3Service newS3Service() {
    AWSCredentials credentials = new AWSCredentials(getS3AccessKey(), getS3SecretKey());
    return new RestS3Service(credentials);
  }

  public Path getPrefix() {
    return prefix;
  }

  public Path getCacheDirectory() {
    return cacheDirectory;
  }

  public Path getOutputDirectory() {
    return outputDirectory;
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }
}
