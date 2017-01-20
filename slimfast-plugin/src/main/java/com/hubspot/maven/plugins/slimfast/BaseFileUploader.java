package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

public abstract class BaseFileUploader implements FileUploader {
  private Set<S3Artifact> s3Artifacts;
  private Path prefix;
  private Path outputFile;

  @Override
  public final void init(UploadConfiguration config, Log log) {
    this.s3Artifacts = Collections.synchronizedSet(new LinkedHashSet<S3Artifact>());
    this.prefix = config.getPrefix();
    this.outputFile = config.getOutputFile();
    doInit(config, log);
  }

  protected abstract void doUpload(String bucket, String key, Path path) throws MojoFailureException, MojoExecutionException;

  protected void doInit(UploadConfiguration config, Log log) {
    // empty by default
  }

  protected void doDestroy() throws MojoFailureException {
    // empty by default
  }

  @Override
  public void upload(UploadConfiguration config, LocalArtifact artifact) throws MojoExecutionException, MojoFailureException {
    String file = artifact.getTargetPath().toString();
    boolean isUnresolvedSnapshot = file.toUpperCase().endsWith("-SNAPSHOT.JAR");

    final String s3Key;
    if (isUnresolvedSnapshot) {
      if (config.isAllowUnresolvedSnapshots()) {
        long timestamp = System.currentTimeMillis();
        String start = file.substring(0, file.length() - ".JAR".length());
        String end = file.substring(file.length() - ".JAR".length());
        s3Key = Paths.get(config.getS3ArtifactRoot()).resolve(start + "-" + timestamp + end).toString();
      } else {
        throw new MojoExecutionException("Encountered unresolved snapshot: " + file);
      }
    } else {
      s3Key = Paths.get(config.getS3ArtifactRoot()).resolve(file).toString();
    }

    Path localPath = artifact.getLocalPath();
    doUpload(config.getS3Bucket(), s3Key, localPath);

    String targetPath = prefix.resolve(artifact.getTargetPath()).toString();
    s3Artifacts.add(new S3Artifact(config.getS3Bucket(), s3Key, targetPath, FileHelper.md5(localPath), FileHelper.size(localPath)));
  }

  @Override
  public final void destroy() throws MojoFailureException {
    try {
      JsonHelper.writeArtifactsToJson(outputFile.toFile(), new S3ArtifactWrapper(prefix.toString(), s3Artifacts));
    } catch (IOException e) {
      throw new MojoFailureException("Error writing dependencies json to file", e);
    }

    doDestroy();
  }
}
