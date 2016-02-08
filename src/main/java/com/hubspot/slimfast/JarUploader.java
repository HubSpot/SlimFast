package com.hubspot.slimfast;

public interface JarUploader {
  void upload(Configuration config, String repositoryPath, String jar) throws Exception;
}
