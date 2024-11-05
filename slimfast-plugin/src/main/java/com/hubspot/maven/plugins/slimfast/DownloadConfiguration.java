package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

public class DownloadConfiguration {

  private final Path prefix;
  private final Path cacheDirectory;
  private final Path outputDirectory;

  private final S3Configuration s3Configuration;

  public DownloadConfiguration(
    S3Configuration s3Configuration,
    Path prefix,
    Path cacheDirectory,
    Path outputDirectory
  ) {
    this.s3Configuration = s3Configuration;
    this.prefix = prefix;
    this.cacheDirectory = cacheDirectory;
    this.outputDirectory = outputDirectory;
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

  public S3Configuration getS3Configuration() {
    return s3Configuration;
  }
}
