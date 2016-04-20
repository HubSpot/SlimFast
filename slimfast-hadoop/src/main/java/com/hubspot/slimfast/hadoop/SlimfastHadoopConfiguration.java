package com.hubspot.slimfast.hadoop;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.hadoop.conf.Configuration;

public class SlimfastHadoopConfiguration {
  private final Path jarDirectory;
  private final Path hdfsArtifactRoot;
  private final Configuration configuration;

  private SlimfastHadoopConfiguration(Path jarDirectory, Path hdfsArtifactRoot, Configuration configuration) {
    this.jarDirectory = jarDirectory;
    this.hdfsArtifactRoot = hdfsArtifactRoot;
    this.configuration = configuration;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Path getJarDirectory() {
    return jarDirectory;
  }

  public Path getHdfsArtifactRoot() {
    return hdfsArtifactRoot;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public static class Builder {
    private Path jarDirectory;
    private Path hdfsArtifactRoot = Paths.get("jars");
    private Configuration configuration;

    public Builder setJarByClass(Class<?> jarClass) {
      URL url = jarClass.getResource("/" + jarClass.getName().replace('.', '/') + ".class");
      if (url == null) {
        throw new IllegalStateException("Could not find resource " + jarClass);
      }

      String qualifiedPath = url.toString();
      if (!qualifiedPath.startsWith("jar:file:")) {
        throw new IllegalStateException("Class doesn't appear to be in a JAR, are you running from a JAR?");
      }

      String jarPath = qualifiedPath.substring("jar:file:".length(), qualifiedPath.indexOf('!'));
      return setJarDirectory(Paths.get(jarPath).getParent());
    }

    public Builder setJarDirectory(Path jarDirectory) {
      this.jarDirectory = jarDirectory;
      return this;
    }

    public Builder setHdfsArtifactRoot(Path hdfsArtifactRoot) {
      this.hdfsArtifactRoot = hdfsArtifactRoot;
      return this;
    }

    public Builder setConfiguration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public SlimfastHadoopConfiguration build() {
      if (jarDirectory == null) {
        throw new IllegalStateException("jarDirectory must be set");
      } else if (hdfsArtifactRoot == null) {
        throw new IllegalStateException("hdfsArtifactRoot must be set");
      } else if (configuration == null) {
        throw new IllegalStateException("configuration must be set");
      }

      return new SlimfastHadoopConfiguration(jarDirectory, hdfsArtifactRoot, configuration);
    }
  }
}
