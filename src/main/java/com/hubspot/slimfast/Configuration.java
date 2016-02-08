package com.hubspot.slimfast;

import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import java.util.Properties;

public class Configuration {
  private final String classpathPrefix;
  private final String s3Bucket;
  private final String s3ArtifactRoot;
  private final String s3AccessKey;
  private final String s3SecretKey;
  private final int s3UploadThreads;
  private final int s3DownloadThreads;

  public Configuration(Properties properties) {
    this.classpathPrefix = checkNotNull(properties.getProperty("classpath.prefix"));
    this.s3Bucket = checkNotNull(properties.getProperty("s3.bucket"));
    this.s3ArtifactRoot = checkNotNull(properties.getProperty("s3.artifact.root"));
    this.s3AccessKey = checkNotNull(properties.getProperty("s3.access.key"));
    this.s3SecretKey = checkNotNull(properties.getProperty("s3.secret.key"));
    this.s3UploadThreads = Integer.parseInt(properties.getProperty("s3.upload.threads", "10"));
    this.s3DownloadThreads = Integer.parseInt(properties.getProperty("s3.download.threads", "10"));
  }

  public S3Service newS3Service() {
    AWSCredentials credentials = new AWSCredentials(getS3AccessKey(), getS3SecretKey());
    return new RestS3Service(credentials);
  }

  public String getClasspathPrefix() {
    return classpathPrefix;
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

  public int getS3UploadThreads() {
    return s3UploadThreads;
  }

  public int getS3DownloadThreads() {
    return s3DownloadThreads;
  }

  private static String checkNotNull(String s) {
    if (s == null) {
      throw new NullPointerException();
    } else {
      return s;
    }
  }
}
