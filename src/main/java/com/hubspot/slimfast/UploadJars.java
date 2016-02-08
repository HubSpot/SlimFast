package com.hubspot.slimfast;

import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.HttpException;
import org.jets3t.service.model.S3Object;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class UploadJars {
  private static final String REPOSITORY_PREFIX = System.getProperty("user.home") + "/.m2/repository/";

  public static void main(String... args) throws Exception {
    Manifest manifest = Utils.readManifest();
    Configuration config = Utils.readConfiguration();
    List<String> jars = Utils.parseClassPath(manifest, config);

    ExecutorService executor = Executors.newFixedThreadPool(config.getS3UploadThreads());
    S3Service s3Service = config.newS3Service();

    List<Future<?>> futures = jars.stream()
        .map(jar -> executor.submit(new UploadJarTask(s3Service, config, jar)))
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

  private static class UploadJarTask implements Callable<Object> {
    private final S3Service s3Service;
    private final String s3Bucket;
    private final String s3Key;
    private final String localPath;

    public UploadJarTask(S3Service s3Service, Configuration config, String jar) {
      this.s3Service = s3Service;
      this.s3Bucket = config.getS3Bucket();
      this.s3Key = config.getS3ArtifactRoot() + "/" + jar;
      this.localPath = REPOSITORY_PREFIX + jar;
    }

    @Override
    public Object call() throws Exception {
      S3Object s3Object = new S3Object(s3Key);
      s3Object.setDataInputFile(new File(localPath));

      try {
        s3Service.getObjectDetails(s3Bucket, s3Key);
        System.out.println("Key already exists " + s3Key);
        return null;
      } catch (ServiceException e) {
        Throwable cause = e.getCause();
        if (!(cause instanceof HttpException) || ((HttpException) cause).getResponseCode() != 404) {
          throw e;
        }
      }

      s3Service.putObject(s3Bucket, s3Object);
      System.out.println("Successfully uploaded key " + s3Key);
      return null;
    }
  }
}
