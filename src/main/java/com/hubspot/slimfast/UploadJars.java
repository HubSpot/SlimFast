package com.hubspot.slimfast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class UploadJars {

  public static void main(String... args) throws Exception {
    String repositoryPath = args[0];

    Manifest manifest = Utils.readManifest();
    Configuration config = Utils.readConfiguration();
    List<String> jars = Utils.parseClassPath(manifest, config);

    ExecutorService executor = Executors.newFixedThreadPool(config.getS3UploadThreads());
    JarUploader uploader = Utils.findImplementation(JarUploader.class, new DefaultJarUploader());

    List<Future<?>> futures = jars.stream()
        .map(jar -> executor.submit(() -> {
          uploader.upload(config, repositoryPath, jar);
          return null;
        }))
        .collect(Collectors.toList());

    executor.shutdown();
    if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
      System.out.println("Took more than 5 minutes to upload JARs, quitting!");
      System.exit(1);
    }

    for (Future<?> future : futures) {
      future.get();
    }
  }
}
