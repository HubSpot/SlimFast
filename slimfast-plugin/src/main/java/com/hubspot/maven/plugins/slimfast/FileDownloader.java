package com.hubspot.maven.plugins.slimfast;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

public interface FileDownloader {
  void init(DownloadConfiguration config, Log log)
    throws MojoExecutionException, MojoFailureException;
  void download(DownloadConfiguration config, S3Artifact artifact)
    throws MojoExecutionException, MojoFailureException;
  void destroy() throws MojoExecutionException, MojoFailureException;
}
