package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

public class FileHelper {

  public static String md5(Path path) throws MojoExecutionException {
    try {
      return Files.hash(path.toFile(), Hashing.md5()).toString();
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading file at path: " + path, e);
    }
  }

  public static long size(Path path) throws MojoExecutionException {
    try {
      return java.nio.file.Files.size(path);
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading file at path: " + path, e);
    }
  }
}
