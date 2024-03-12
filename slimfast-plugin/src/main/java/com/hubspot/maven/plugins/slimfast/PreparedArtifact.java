package com.hubspot.maven.plugins.slimfast;

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
}
