package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

public class DownloadConfiguration {

  private final Path prefix;
  private final Path cacheDirectory;
  private final Path outputDirectory;
  private final String s3AccessKey;
  private final String s3SecretKey;

  public DownloadConfiguration(
    Path prefix,
    Path cacheDirectory,
    Path outputDirectory,
    String s3AccessKey,
    String s3SecretKey
  ) {
    this.prefix = prefix;
    this.cacheDirectory = cacheDirectory;
    this.outputDirectory = outputDirectory;
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
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
