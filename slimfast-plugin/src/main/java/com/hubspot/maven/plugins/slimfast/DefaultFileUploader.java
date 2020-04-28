package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkBaseException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class DefaultFileUploader extends BaseFileUploader {
  private AmazonS3 s3Service;
  private TransferManager transferManager;
  private Log log;

  @Override
  protected void doInit(UploadConfiguration config, Log log) {
    this.s3Service = S3Factory.create(config.getS3AccessKey(), config.getS3SecretKey());
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("slimfast-upload").setDaemon(true).build();
    transferManager = TransferManagerBuilder.standard()
        .withS3Client(s3Service)
        .withExecutorFactory(() -> Executors.newFixedThreadPool(config.getUploadThreads(), threadFactory))
        .build();
    this.log = log;
  }

  @Override
  protected void doUpload(String bucket, String key, Path path) throws MojoFailureException, MojoExecutionException {
    if (keyExists(bucket, key)) {
      log.info("Key already exists " + key);
      return;
    }

    try {
      Upload upload = transferManager.upload(bucket, key, path.toFile());
      upload.waitForUploadResult();
      log.info("Successfully uploaded key " + key);
    } catch (SdkClientException | InterruptedException e) {
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
