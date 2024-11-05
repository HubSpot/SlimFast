package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PreparedArtifactWrapper implements ArtifactWrapper {

  private final Path prefix;
  private final Set<PreparedArtifact> artifacts;

  public PreparedArtifactWrapper(Path prefix, Set<PreparedArtifact> artifacts) {
    this.prefix = prefix;
    this.artifacts = artifacts;
  }

  public Path getPrefix() {
    return prefix;
  }

  public Set<PreparedArtifact> getArtifacts() {
    return artifacts;
  }

  @Override
  public Set<LocalArtifact> getLocalArtifacts() {
    return artifacts == null
      ? Set.of()
      : artifacts
        .stream()
        .map(PreparedArtifact::toLocalArtifact)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
