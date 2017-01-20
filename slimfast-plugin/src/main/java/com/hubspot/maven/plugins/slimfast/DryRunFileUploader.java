package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

public class DryRunFileUploader extends BaseFileUploader {

  @Override
  protected void doUpload(String bucket, String key, Path path) {
    // do nothing
  }
}
