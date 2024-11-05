package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
import java.util.Set;

public interface ArtifactWrapper {
  Path getPrefix();

  Set<LocalArtifact> getLocalArtifacts();
}
