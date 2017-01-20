package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.HttpException;
import org.jets3t.service.model.S3Object;

public class DefaultFileUploader extends BaseFileUploader {
  private S3Service s3Service;
  private Log log;

  @Override
  protected void doInit(UploadConfiguration config, Log log) {
    this.s3Service = config.newS3Service();
    this.log = log;
  }

  @Override
  protected void doUpload(String bucket, String key, Path path) throws MojoFailureException, MojoExecutionException {
    if (keyExists(bucket, key)) {
      log.info("Key already exists " + key);
      return;
    }

    S3Object s3Object = new S3Object(key);
    s3Object.setDataInputFile(path.toFile());
    try {
      s3Object.setContentLength(java.nio.file.Files.size(path));
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading file at path: " + path, e);
    }

    try {
      s3Service.putObject(bucket, s3Object);
      log.info("Successfully uploaded key " + key);
    } catch (ServiceException e) {
      throw new MojoFailureException("Error uploading file " + path, e);
    }
  }

  @Override
  protected void doDestroy() throws MojoFailureException {
    try {
      s3Service.shutdown();
    } catch (ServiceException e) {
      throw new MojoFailureException("Error closing S3Service", e);
    }
  }

  private boolean keyExists(String bucket, String key) throws MojoFailureException {
    try {
      s3Service.getObjectDetails(bucket, key);
      return true;
    } catch (ServiceException e) {
      Throwable cause = e.getCause();
      if (cause instanceof HttpException && ((HttpException) cause).getResponseCode() == 404) {
        return false;
      } else {
        throw new MojoFailureException("Error getting object details for key " + key, e);
      }
    }
  }
}
