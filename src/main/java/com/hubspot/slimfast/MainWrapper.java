package com.hubspot.slimfast;

import java.lang.reflect.Method;
import java.util.jar.Manifest;

public class MainWrapper {

  public static void main(String... args) throws Exception {
    DownloadJars.main(args);

    Manifest manifest = Utils.readManifest();
    String realMainClassName = manifest.getMainAttributes().getValue("Real-Main-Class");
    if (realMainClassName == null) {
      throw new IllegalStateException("Manifest does not contain a Real-Main-Class attribute");
    }

    Class<?> realMainClass = Class.forName(realMainClassName);
    final Method mainMethod;
    try {
      mainMethod = realMainClass.getMethod("main", String[].class);
    } catch (Exception e) {
      throw new RuntimeException("Could not find main method for class " + realMainClass.getName(), e);
    }

    mainMethod.invoke(null, (String[]) args);
  }
}
