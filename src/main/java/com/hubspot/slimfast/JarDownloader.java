package com.hubspot.slimfast;

public interface JarDownloader {
  void download(Configuration config, String jar) throws Exception;
}
