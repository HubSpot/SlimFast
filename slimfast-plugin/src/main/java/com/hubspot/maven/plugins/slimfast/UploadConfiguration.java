package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

public class UploadConfiguration {
  private final Path prefix;
  private final String s3Bucket;
  private final String s3ArtifactRoot;
  private final String s3AccessKey;
  private final String s3SecretKey;
  private final Path outputFile;
  private final boolean allowUnresolvedSnapshots;

  public UploadConfiguration(Path prefix,
                             String s3Bucket,
                             String s3ArtifactRoot,
                             String s3AccessKey,
                             String s3SecretKey,
                             Path outputFile,
                             boolean allowUnresolvedSnapshots) {
    this.prefix = prefix;
    this.s3Bucket = s3Bucket;
    this.s3ArtifactRoot = s3ArtifactRoot;
    this.s3AccessKey = s3AccessKey;
    this.s3SecretKey = s3SecretKey;
    this.outputFile = outputFile;
    this.allowUnresolvedSnapshots = allowUnresolvedSnapshots;
  }

  public S3Service newS3Service() {
    AWSCredentials credentials = new AWSCredentials(getS3AccessKey(), getS3SecretKey());
    return new RestS3Service(credentials);
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
}
