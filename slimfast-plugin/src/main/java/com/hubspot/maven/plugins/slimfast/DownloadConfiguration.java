package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

import com.amazonaws.services.s3.AmazonS3;

public class DownloadConfiguration {
  private final Path prefix;
  private final Path cacheDirectory;
  private final Path outputDirectory;
  private final String s3AccessKey;
  private final String s3SecretKey;
  private final String s3Endpoint;

  public DownloadConfiguration(Path prefix,
                               Path cacheDirectory,
                               Path outputDirectory,
                               String s3AccessKey,
                               String s3SecretKey,
                               String s3Endpoint) {
    this.prefix = prefix;
    this.cacheDirectory = cacheDirectory;
    this.outputDirectory = outputDirectory;
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
    this.s3Endpoint = s3Endpoint;
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

  public String getS3Endpoint(){
    return s3Endpoint;
  }
}
