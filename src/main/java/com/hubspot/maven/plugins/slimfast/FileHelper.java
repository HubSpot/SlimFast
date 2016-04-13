package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;

import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.hash.Hashing;

public class FileHelper {

  public static void ensureDirectoryExists(@Nullable Path path) throws MojoExecutionException {
    if (path != null && !Files.exists(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw new MojoExecutionException("Error creating parent directories for path " + path, e);
      }
    }
  }

  public static String md5(Path path) throws MojoExecutionException {
    try {
      return com.google.common.io.Files.hash(path.toFile(), Hashing.md5()).toString();
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
