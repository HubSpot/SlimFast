package com.hubspot.slimfast;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class DownloadJars {

  public static void main(String... args) throws Exception {
    Manifest manifest = Utils.readManifest();
    Configuration config = Utils.readConfiguration();
    List<String> jars = Utils.parseClassPath(manifest, config);

    ExecutorService executor = Executors.newFixedThreadPool(config.getS3DownloadThreads());
    JarDownloader downloader = findJarDownloader();

    List<Future<?>> futures = jars.stream()
        .map(jar -> executor.submit(() -> {
          downloader.download(config, jar);
          return null;
        }))
        .collect(Collectors.toList());

    executor.shutdown();
    if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
      System.out.println("Took more than 5 minutes to download JARs, quitting!");
      System.exit(1);
    }

    for (Future<?> future : futures) {
      future.get();
    }
  }

  private static JarDownloader findJarDownloader() {
    List<JarDownloader> downloaders = new ArrayList<>();
    for (JarDownloader downloader : ServiceLoader.load(JarDownloader.class)) {
      downloaders.add(downloader);
    }

    if (downloaders.isEmpty()) {
      return new DefaultJarDownloader();
    } else if (downloaders.size() > 1) {
      throw new IllegalStateException("Multiple JAR downloaders found: " + downloaders);
    } else {
      return downloaders.get(0);
    }
  }
}
