package com.hubspot.maven.plugins.slimfast;

import java.io.Closeable;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public interface FileDownloader extends Closeable {
  void init(DownloadConfiguration config)
    throws MojoExecutionException, MojoFailureException;
  void download(Set<S3Artifact> artifact)
    throws MojoExecutionException, MojoFailureException;
}
