package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
import java.util.Set;

public class LocalArtifactWrapper {
  private final Path prefix;
  private final Set<LocalArtifact> artifacts;

  public LocalArtifactWrapper(Path prefix, Set<LocalArtifact> artifacts) {
    this.prefix = prefix;
    this.artifacts = artifacts;
  }

  public Path getPrefix() {
    return prefix;
  }

  public Set<LocalArtifact> getArtifacts() {
    return artifacts;
  }
}
