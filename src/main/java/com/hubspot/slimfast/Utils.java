package com.hubspot.slimfast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

  public static Manifest readManifest() throws IOException {
    for (URL url : Collections.list(classLoader().getResources("META-INF/MANIFEST.MF"))) {
      try (InputStream manifestStream = url.openStream()) {
        Manifest manifest = new Manifest(manifestStream);
        if (manifest.getMainAttributes().getValue("Class-Path") != null) {
          return manifest;
        }
      }
    }

    throw new IllegalStateException("Could not find manifest with a Class-Path attribute");
  }

  public static Configuration readConfiguration() throws IOException {
    try (InputStream propertiesStream = classLoader().getResourceAsStream("slimfast.properties")) {
      Properties properties = new Properties();
      properties.load(propertiesStream);
      return new Configuration(properties);
    }
  }

  public static List<String> parseClassPath(Manifest manifest, Configuration config) {
    String classPath = manifest.getMainAttributes().getValue("Class-Path");

    return Stream.of(classPath.split(" "))
        .map(path -> {
          if (!path.startsWith(config.getClasspathPrefix())) {
            throw new IllegalArgumentException("Expected to start with " + config.getClasspathPrefix() + " but was " + path);
          } else {
            return path.substring(config.getClasspathPrefix().length());
          }
        }).collect(Collectors.toList());
  }

  public static Path jarDirectory() {
    CodeSource codeSource = DownloadJars.class.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      throw new RuntimeException("Cannot determine JAR directory, are you running from a JAR?");
    }

    try {
      return new File(codeSource.getLocation().toURI().getPath()).toPath().getParent();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T findImplementation(Class<T> type, T defaultImplementation) {
    List<T> implementations = new ArrayList<>();
    for (T implementation : ServiceLoader.load(type)) {
      implementations.add(implementation);
    }

    if (implementations.isEmpty()) {
      return defaultImplementation;
    } else if (implementations.size() > 1) {
      throw new IllegalStateException("Multiple implementations found: " + implementations);
    } else {
      return implementations.get(0);
    }
  }

  private static ClassLoader classLoader() {
    return Utils.class.getClassLoader();
  }
}
