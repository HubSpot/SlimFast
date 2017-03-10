package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkBaseException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;

public class DefaultFileUploader extends BaseFileUploader {
  private AmazonS3 s3Service;
  private Log log;

  @Override
  protected void doInit(UploadConfiguration config, Log log) {
    this.s3Service = S3Factory.create(config.getS3AccessKey(), config.getS3SecretKey());
    this.log = log;
  }

  @Override
  protected void doUpload(String bucket, String key, Path path) throws MojoFailureException, MojoExecutionException {
    if (keyExists(bucket, key)) {
      log.info("Key already exists " + key);
      return;
    }

    try {
      s3Service.putObject(bucket, key, path.toFile());
      log.info("Successfully uploaded key " + key);
    } catch (SdkClientException e) {
      throw new MojoFailureException("Error uploading file " + path, e);
    }
  }

  @Override
  protected void doDestroy() throws MojoFailureException {}

  private boolean keyExists(String bucket, String key) throws MojoFailureException {
    try {
      s3Service.getObjectMetadata(bucket, key);
      return true;
    } catch (SdkBaseException e) {
      if (e instanceof AmazonServiceException && ((AmazonServiceException) e).getStatusCode() == 404) {
        return false;
      } else {
        throw new MojoFailureException("Error getting object metadata for key " + key, e);
      }
    }
  }
}
