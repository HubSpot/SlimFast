package com.hubspot.slimfast.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Manifest;

public class HadoopHelper {
  private static final Logger LOG = LoggerFactory.getLogger(HadoopHelper.class);

  public static void writeJarsToHdfsAndAddToClasspath(SlimfastHadoopConfiguration slimfastConfiguration) {
    try {
      FileSystem hdfs = FileSystem.get(slimfastConfiguration.getConfiguration());

      for (String jar : findClasspathJars(slimfastConfiguration.getJarDirectory())) {
        Path destination = new Path(slimfastConfiguration.getHdfsArtifactRoot().resolve(jar).toString());
        if (exists(hdfs, destination)) {
          LOG.info("Path already exists {}", destination);
        } else {
          Path source = new Path(slimfastConfiguration.getJarDirectory().resolve(jar).toString());
          hdfs.copyFromLocalFile(source, destination);
          LOG.info("Successfully uploaded path {}", destination);
        }

        addJarToJobConfiguration(destination, slimfastConfiguration.getConfiguration());
      }
    } catch (IOException e) {
      throw new RuntimeException("Error writing JARs to HDFS", e);
    }
  }

  private static void addJarToJobConfiguration(Path jarPath, Configuration configuration) throws IOException {
    String jar = jarPath.toString();
    String existingClasspath = configuration.get("mapreduce.job.classpath.files");
    String updatedClasspath = existingClasspath == null ? jar : existingClasspath + "," + jar;
    configuration.set("mapreduce.job.classpath.files", updatedClasspath);

    jar = FileSystem.get(configuration).makeQualified(jarPath).toUri().toString();
    String existingCacheFiles = configuration.get("mapreduce.job.cache.files");
    String updatedCacheFiles = existingCacheFiles == null ? jar : existingCacheFiles + "," + jar;
    configuration.set("mapreduce.job.cache.files", updatedCacheFiles);
  }

  private static boolean exists(FileSystem fileSystem, Path path) throws IOException {
    try {
      FileStatus status = fileSystem.getFileStatus(path);
      return status != null && status.getLen() > 0;
    } catch (FileNotFoundException e) {
      return false;
    }
  }

  private static Set<String> findClasspathJars(java.nio.file.Path jarDirectory) throws IOException {
    Set<String> classpathJars = new LinkedHashSet<>();
    for (URL url : Collections.list(getClassLoader().getResources("META-INF/MANIFEST.MF"))) {
      try (InputStream manifestStream = url.openStream()) {
        Manifest manifest = new Manifest(manifestStream);
        String classPath = manifest.getMainAttributes().getValue("Class-Path");
        if (classPath != null) {
          for (String jar : classPath.split(" ")) {
            if (jarDirectory.resolve(jar).toFile().exists()) {
              classpathJars.add(jar);
            }
          }
        }
      }
    }

    return classpathJars;
  }

  private static ClassLoader getClassLoader() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    return contextClassLoader == null ? HadoopHelper.class.getClassLoader() : contextClassLoader;
  }
}
