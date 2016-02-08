package com.hubspot.slimfast;

public interface JarDownloader {
  void download(Configuration configuration, String jar) throws Exception;
}
