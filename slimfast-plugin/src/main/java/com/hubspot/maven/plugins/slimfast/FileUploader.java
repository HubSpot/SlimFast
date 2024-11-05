package com.hubspot.maven.plugins.slimfast;

import java.io.Closeable;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public interface FileUploader extends Closeable {
  void init(UploadConfiguration config);
  Set<S3Artifact> upload(Set<LocalArtifact> artifacts)
    throws MojoExecutionException, MojoFailureException;
}
