package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public abstract class BaseFileUploader implements FileUploader {

  private UploadConfiguration config;

  @Override
  public void init(UploadConfiguration config) {
    this.config = config;
  }

  protected abstract void doUpload(Set<S3Artifact> artifacts)
    throws ExecutionException, InterruptedException, TimeoutException;

  @Override
  public Set<S3Artifact> upload(Set<LocalArtifact> artifacts)
    throws MojoExecutionException, MojoFailureException {
    Set<S3Artifact> s3Artifacts = artifacts
      .stream()
      .map(artifact ->
        new S3Artifact(
          config.getS3Bucket(),
          getS3Key(artifact),
          artifact.getLocalPath(),
          config.getPrefix().resolve(artifact.getTargetPath()).toString(),
          FileHelper.md5(artifact.getLocalPath()),
          FileHelper.size(artifact.getLocalPath())
        )
      )
      .collect(Collectors.toCollection(LinkedHashSet::new));

    try {
      doUpload(s3Artifacts);
    } catch (Exception e) {
      throw new MojoExecutionException("Failed to upload artifacts", e);
    }

    try {
      JsonHelper.writeArtifactsToJson(
        config.getOutputFile().toFile(),
        new S3ArtifactWrapper(config.getPrefix().toString(), s3Artifacts)
      );
    } catch (IOException e) {
      throw new MojoExecutionException("Error writing dependencies json to file", e);
    }

    return s3Artifacts;
  }

  private String getS3Key(LocalArtifact artifact) {
    final String s3Key;

    String file = artifact.getTargetPath().toString();
    boolean isUnresolvedSnapshot = file.toUpperCase().endsWith("-SNAPSHOT.JAR");

    if (isUnresolvedSnapshot) {
      if (config.isAllowUnresolvedSnapshots()) {
        String start = file.substring(0, file.length() - ".JAR".length());
        String end = file.substring(file.length() - ".JAR".length());
        String md5 = FileHelper.md5(artifact.getLocalPath());
        s3Key =
          Paths
            .get(config.getS3ArtifactRoot())
            .resolve(start + "-" + md5 + end)
            .toString();
      } else {
        throw new IllegalStateException("Encountered unresolved snapshot: " + file);
      }
    } else {
      s3Key = Paths.get(config.getS3ArtifactRoot()).resolve(file).toString();
    }

    return s3Key;
  }
}
