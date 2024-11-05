package com.hubspot.maven.plugins.slimfast;

import java.util.Optional;
import software.amazon.awssdk.regions.Region;

public class S3Configuration {

  private final Optional<String> accessKey;
  private final Optional<String> secretKey;

  private final Optional<Region> region;
  private final Optional<Double> targetThroughputGbps;
  private final Optional<Long> minPartSizeBytes;

  public S3Configuration(
    Optional<String> accessKey,
    Optional<String> secretKey,
    Optional<Region> region,
    Optional<Double> targetThroughputGbps,
    Optional<Long> minPartSizeBytes
  ) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.region = region;
    this.targetThroughputGbps = targetThroughputGbps;
    this.minPartSizeBytes = minPartSizeBytes;
  }

  public Optional<String> getAccessKey() {
    return accessKey;
  }

  public Optional<String> getSecretKey() {
    return secretKey;
  }

  public Optional<Region> getRegion() {
    return region;
  }

  public Optional<Double> getTargetThroughputGbps() {
    return targetThroughputGbps;
  }

  public Optional<Long> getMinPartSizeBytes() {
    return minPartSizeBytes;
  }
}
