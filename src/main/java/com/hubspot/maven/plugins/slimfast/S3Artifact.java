package com.hubspot.maven.plugins.slimfast;

public class S3Artifact {
  private final String bucket;
  private final String key;
  private final String targetPath;
  private final String md5;
  private final long size;

  public S3Artifact(String bucket, String key, String targetPath, String md5, long size) {
    this.bucket = bucket;
    this.key = key;
    this.targetPath = targetPath;
    this.md5 = md5;
    this.size = size;
  }

  public String getBucket() {
    return bucket;
  }

  public String getKey() {
    return key;
  }

  public String getTargetPath() {
    return targetPath;
  }

  public String getMd5() {
    return md5;
  }

  public long getSize() {
    return size;
  }
}
