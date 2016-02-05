package com.hubspot.slimfast;

import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class DownloadJars {
  private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

  public static void main(String... args) throws Exception {
    Manifest manifest = Utils.readManifest();
    List<String> jars = Utils.parseClassPath(manifest);
    Properties s3Properties = Utils.readS3Properties();

    AWSCredentials credentials = new AWSCredentials(s3Properties.getProperty("s3.access.key"), s3Properties.getProperty("s3.secret.key"));
    S3Service s3Service = new RestS3Service(credentials);

    List<Future<?>> futures = jars.stream()
        .map(jar -> EXECUTOR.submit(new DownloadJarTask(s3Service, s3Properties, jar)))
        .collect(Collectors.toList());

    EXECUTOR.shutdown();
    if (!EXECUTOR.awaitTermination(5, TimeUnit.MINUTES)) {
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

    public DownloadJarTask(S3Service s3Service, Properties s3Properties, String jar) {
      this.s3Service = s3Service;
      this.s3Bucket = s3Properties.getProperty("s3.bucket");
      this.s3Key = s3Properties.getProperty("s3.artifact.root") + "/" + jar;
      this.localPath = jarDirectory().resolve(Utils.CLASSPATH_PREFIX).resolve(jar);;
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
