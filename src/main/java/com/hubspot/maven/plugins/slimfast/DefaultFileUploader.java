package com.hubspot.maven.plugins.slimfast;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.HttpException;
import org.jets3t.service.model.S3Object;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DefaultFileUploader implements FileUploader {
  private S3Service s3Service;
  private Log log;

  @Override
  public void init(Configuration config, Log log) {
    this.s3Service = config.newS3Service();
    this.log = log;
  }

  @Override
  public void upload(Configuration config, String file) throws MojoExecutionException, MojoFailureException {
    String s3Key = config.getS3ArtifactRoot() + "/" + file;
    Path localPath = Paths.get(config.getRepositoryPath()).resolve(file);

    S3Object s3Object = new S3Object(s3Key);
    s3Object.setDataInputFile(localPath.toFile());
    try {
      s3Object.setContentLength(Files.size(localPath));
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading file at path: " + localPath, e);
    }

    try {
      s3Service.getObjectDetails(config.getS3Bucket(), s3Key);
      log.info("Key already exists " + s3Key);
      return;
    } catch (ServiceException e) {
      Throwable cause = e.getCause();
      if (!(cause instanceof HttpException) || ((HttpException) cause).getResponseCode() != 404) {
        throw new MojoFailureException("Error getting object details for key " + s3Key, e);
      }
    }

    try {
      s3Service.putObject(config.getS3Bucket(), s3Object);
    } catch (ServiceException e) {
      throw new MojoFailureException("Error uploading file " + localPath, e);
    }
    log.info("Successfully uploaded key " + s3Key);
  }

  @Override
  public void destroy() throws MojoFailureException {
    try {
      s3Service.shutdown();
    } catch (ServiceException e) {
      throw new MojoFailureException("Error closing S3Service", e);
    }
  }
}
