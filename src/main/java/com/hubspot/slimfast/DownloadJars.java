package com.hubspot.slimfast;

import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Object;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.List;
import java.util.concurrent.Callable;
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
    S3Service s3Service = config.newS3Service();

    List<Future<?>> futures = jars.stream()
        .map(jar -> executor.submit(new DownloadJarTask(s3Service, config, jar)))
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

  private static Path jarDirectory() {
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

  private static class DownloadJarTask implements Callable<Object> {
    private final S3Service s3Service;
    private final String s3Bucket;
    private final String s3Key;
    private final Path localPath;

    public DownloadJarTask(S3Service s3Service, Configuration config, String jar) {
      this.s3Service = s3Service;
      this.s3Bucket = config.getS3Bucket();
      this.s3Key = config.getS3ArtifactRoot() + "/" + jar;
      this.localPath = jarDirectory().resolve(config.getClasspathPrefix()).resolve(jar);;
    }

    @Override
    public Object call() throws Exception {
      Path parent = localPath.getParent();
      if (localPath.toFile().exists()) {
        System.out.println("JAR path already exists " + localPath);
        return null;
      } else if (parent != null) {
        Files.createDirectories(parent);
      }

      S3Object s3Object = s3Service.getObject(s3Bucket, s3Key);

      Files.copy(s3Object.getDataInputStream(), localPath);
      System.out.println("Successfully downloaded key " + s3Key);
      return null;
    }
  }
}
