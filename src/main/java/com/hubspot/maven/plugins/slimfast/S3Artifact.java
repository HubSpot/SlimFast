package com.hubspot.maven.plugins.slimfast;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    S3Artifact artifact = (S3Artifact) o;
    return Objects.equals(size, artifact.size) &&
        Objects.equals(bucket, artifact.bucket) &&
        Objects.equals(key, artifact.key) &&
        Objects.equals(targetPath, artifact.targetPath) &&
        Objects.equals(md5, artifact.md5);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bucket, key, targetPath, md5, size);
  }
}
