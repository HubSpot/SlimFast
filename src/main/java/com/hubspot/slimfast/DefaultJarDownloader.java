package com.hubspot.slimfast;

import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Object;

import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultJarDownloader implements JarDownloader {
  private volatile S3Service s3Service;
  private volatile Path jarDirectory;

  @Override
  public void download(Configuration config, String jar) throws Exception {
    if (s3Service == null) {
      synchronized (this) {
        if (s3Service == null) {
          s3Service = config.newS3Service();
          jarDirectory = Utils.jarDirectory();
        }
      }
    }

    Path localPath = jarDirectory.resolve(config.getClasspathPrefix()).resolve(jar);
    Path parent = localPath.getParent();
    if (localPath.toFile().exists()) {
      System.out.println("JAR path already exists " + localPath);
      return;
    } else if (parent != null) {
      Files.createDirectories(parent);
    }

    String s3Key = config.getS3ArtifactRoot() + "/" + jar;
    S3Object s3Object = s3Service.getObject(config.getS3Bucket(), s3Key);

    Files.copy(s3Object.getDataInputStream(), localPath);
    System.out.println("Successfully downloaded key " + s3Key);
  }
}
