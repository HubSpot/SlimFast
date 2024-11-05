package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PreparedArtifact {

  private final String localPath;
  private final String targetPath;

  public PreparedArtifact(String localPath, String targetPath) {
    this.localPath = localPath;
    this.targetPath = targetPath;
  }

  public String getLocalPath() {
    return localPath;
  }

  public String getTargetPath() {
    return targetPath;
  }

  public LocalArtifact toLocalArtifact() {
    Path targetPath = Paths.get(getTargetPath());
    Path localPath = Paths.get(getLocalPath());

    return new LocalArtifact(localPath, targetPath);
  }
}
