package com.hubspot.slimfast;

import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.HttpException;
import org.jets3t.service.model.S3Object;

import java.io.File;

public class DefaultJarUploader implements JarUploader {
  private volatile S3Service s3Service;

  @Override
  public void upload(Configuration config, String repositoryPath, String jar) throws Exception {
    if (s3Service == null) {
      synchronized (this) {
        if (s3Service == null) {
          s3Service = config.newS3Service();
        }
      }
    }

    String s3Key = config.getS3ArtifactRoot() + "/" + jar;
    String localPath = repositoryPath + (repositoryPath.endsWith("/") ? "" : "/") + jar;

    S3Object s3Object = new S3Object(s3Key);
    s3Object.setDataInputFile(new File(localPath));

    try {
      s3Service.getObjectDetails(config.getS3Bucket(), s3Key);
      System.out.println("Key already exists " + s3Key);
      return;
    } catch (ServiceException e) {
      Throwable cause = e.getCause();
      if (!(cause instanceof HttpException) || ((HttpException) cause).getResponseCode() != 404) {
        throw e;
      }
    }

    s3Service.putObject(config.getS3Bucket(), s3Object);
    System.out.println("Successfully uploaded key " + s3Key);
  }
}
