package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class S3Artifact {

  private final String bucket;
  private final String key;
  private final Optional<Path> localPath;
  private final String targetPath;
  private final String md5;
  private final long size;

  public S3Artifact(String bucket, String key, String targetPath, String md5, long size) {
    this(bucket, key, null, targetPath, md5, size);
  }

  public S3Artifact(
    String bucket,
    String key,
    Path localPath,
    String targetPath,
    String md5,
    long size
  ) {
    this.bucket = bucket;
    this.key = key;
    this.localPath = Optional.ofNullable(localPath);
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

  public Optional<Path> getLocalPath() {
    return localPath;
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
    return (
      Objects.equals(size, artifact.size) &&
      Objects.equals(bucket, artifact.bucket) &&
      Objects.equals(key, artifact.key) &&
      Objects.equals(targetPath, artifact.targetPath) &&
      Objects.equals(md5, artifact.md5)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(bucket, key, targetPath, md5, size);
  }
}
