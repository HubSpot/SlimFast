package com.hubspot.maven.plugins.slimfast;

import java.util.Set;

public class ArtifactWrapper {
  private final String classpathPrefix;
  private final Set<S3Artifact> artifacts;

  public ArtifactWrapper(String classpathPrefix, Set<S3Artifact> artifacts) {
    this.classpathPrefix = classpathPrefix;
    this.artifacts = artifacts;
  }

  public String getClasspathPrefix() {
    return classpathPrefix;
  }

  public Set<S3Artifact> getArtifacts() {
    return artifacts;
  }
}
