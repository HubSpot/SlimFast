package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class DryRunFileUploader extends BaseFileUploader {

  @Override
  protected void doUpload(Set<S3Artifact> artifacts)
    throws ExecutionException, InterruptedException, TimeoutException {}

  @Override
  public void close() throws IOException {}
}
