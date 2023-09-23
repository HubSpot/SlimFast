package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

public class UploadConfiguration {
  private final Path prefix;
  private final String s3Bucket;
  private final String s3ArtifactRoot;
  private final String s3AccessKey;
  private final String s3SecretKey;
  private final Path outputFile;
  private final boolean allowUnresolvedSnapshots;
  private final int uploadThreads;

  public UploadConfiguration(Path prefix,
                             String s3Bucket,
                             String s3ArtifactRoot,
                             String s3AccessKey,
                             String s3SecretKey,
                             Path outputFile,
                             boolean allowUnresolvedSnapshots,
                             int uploadThreads) {
    this.prefix = prefix;
    this.s3Bucket = s3Bucket;
    this.s3ArtifactRoot = s3ArtifactRoot;
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
    this.outputFile = outputFile;
    this.allowUnresolvedSnapshots = allowUnresolvedSnapshots;
    this.uploadThreads = uploadThreads;
  }

  public Path getPrefix() {
    return prefix;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public String getS3ArtifactRoot() {
    return s3ArtifactRoot;
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public Path getOutputFile() {
    return outputFile;
  }

  public boolean isAllowUnresolvedSnapshots() {
    return allowUnresolvedSnapshots;
  }

  public int getUploadThreads() {
    return uploadThreads;
  }
}
