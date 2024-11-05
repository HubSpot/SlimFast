package com.hubspot.maven.plugins.slimfast;

import java.util.Optional;
import software.amazon.awssdk.regions.Region;

public class S3Configuration {

  private final String accessKey;
  private final String secretKey;

  private final Optional<Region> region;
  private final Optional<Double> targetThroughputGbps;
  private final Optional<Long> minPartSizeBytes;

  public S3Configuration(
    String accessKey,
    String secretKey,
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

  public String getAccessKey() {
    return accessKey;
  }

  public String getSecretKey() {
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
