package com.hubspot.maven.plugins.slimfast;

import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.annotation.Nullable;

public class FileHelper {

  public static void ensureDirectoryExists(@Nullable Path path) {
    if (path != null && !Files.exists(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw new UncheckedIOException(
          "Error creating parent directories for path " + path,
          e
        );
      }
    }
  }

  public static void atomicMove(Path sourcePath, Path destPath) {
    try {
      Files.move(sourcePath, destPath, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String md5(Path path) {
    try {
      return com.google.common.io.Files.hash(path.toFile(), Hashing.md5()).toString();
    } catch (IOException e) {
      throw new UncheckedIOException("Error reading file at path: " + path, e);
    }
  }

  public static long size(Path path) {
    try {
      return Files.size(path);
    } catch (IOException e) {
      throw new UncheckedIOException("Error reading file at path: " + path, e);
    }
  }
}
