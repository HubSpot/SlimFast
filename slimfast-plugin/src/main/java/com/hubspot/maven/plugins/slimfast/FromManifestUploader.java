package com.hubspot.maven.plugins.slimfast;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

public interface FromManifestUploader {
  void init(UploadConfiguration config, Log log)
    throws MojoExecutionException, MojoFailureException;
  void uploadFromManifest(UploadConfiguration config, PreparedArtifact artifact)
    throws MojoExecutionException, MojoFailureException;
  void destroy() throws MojoExecutionException, MojoFailureException;
}
