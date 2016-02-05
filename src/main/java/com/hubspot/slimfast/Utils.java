package com.hubspot.slimfast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
  public static final String CLASSPATH_PREFIX = "lib/";

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

  public static Properties readS3Properties() throws IOException {
    try (InputStream propertiesStream = classLoader().getResourceAsStream("slimfast.s3.properties")) {
      Properties s3Properties = new Properties();
      s3Properties.load(propertiesStream);
      return s3Properties;
    }
  }

  public static List<String> parseClassPath(Manifest manifest) {
    String classPath = manifest.getMainAttributes().getValue("Class-Path");

    return Stream.of(classPath.split(" "))
        .map(path -> {
          if (!path.startsWith(CLASSPATH_PREFIX)) {
            throw new IllegalArgumentException("Expected to start with " + CLASSPATH_PREFIX + " but was " + path);
          } else {
            return path.substring(CLASSPATH_PREFIX.length());
          }
        }).collect(Collectors.toList());
  }

  private static ClassLoader classLoader() {
    return Utils.class.getClassLoader();
  }
}
