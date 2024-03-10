package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

public class LocalArtifact {

  private final Path localPath;
  private final Path targetPath;

  public LocalArtifact(Path localPath, Path targetPath) {
    this.localPath = localPath;
    this.targetPath = targetPath;
  }

  public Path getLocalPath() {
    return localPath;
  }

  public Path getTargetPath() {
    return targetPath;
  }
}
