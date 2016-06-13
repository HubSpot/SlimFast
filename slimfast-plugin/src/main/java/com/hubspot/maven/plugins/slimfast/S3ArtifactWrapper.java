package com.hubspot.maven.plugins.slimfast;

import java.util.Set;

public class S3ArtifactWrapper {
  private final String prefix;
  private final Set<S3Artifact> artifacts;

  public S3ArtifactWrapper(String prefix, Set<S3Artifact> artifacts) {
    this.prefix = prefix;
    this.artifacts = artifacts;
  }

  public String getPrefix() {
    return prefix;
  }

  public Set<S3Artifact> getArtifacts() {
    return artifacts;
  }
}
