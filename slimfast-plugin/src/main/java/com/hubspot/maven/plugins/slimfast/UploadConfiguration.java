package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

public class UploadConfiguration {

  private final Path prefix;
  private final String s3Bucket;
  private final String s3ArtifactRoot;
  private final Path outputFile;
  private final boolean allowUnresolvedSnapshots;

  private final S3Configuration s3Configuration;

  public UploadConfiguration(
    S3Configuration s3Configuration,
    Path prefix,
    String s3Bucket,
    String s3ArtifactRoot,
    Path outputFile,
    boolean allowUnresolvedSnapshots
  ) {
    this.s3Configuration = s3Configuration;
    this.prefix = prefix;
    this.s3Bucket = s3Bucket;
    this.s3ArtifactRoot = s3ArtifactRoot;
    this.outputFile = outputFile;
    this.allowUnresolvedSnapshots = allowUnresolvedSnapshots;
  }

  public Path getPrefix() {
    return prefix;
  }

  public S3Configuration getS3Configuration() {
    return s3Configuration;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public String getS3ArtifactRoot() {
    return s3ArtifactRoot;
  }

  public Path getOutputFile() {
    return outputFile;
  }

  public boolean isAllowUnresolvedSnapshots() {
    return allowUnresolvedSnapshots;
  }
}
