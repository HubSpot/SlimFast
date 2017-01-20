package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.HttpException;
import org.jets3t.service.model.S3Object;

public class DefaultFileUploader implements FileUploader {
  private S3Service s3Service;
  private Set<S3Artifact> s3Artifacts;
  private Path prefix;
  private Path outputFile;
  private Log log;

  @Override
  public void init(UploadConfiguration config, Log log) {
    this.s3Service = config.newS3Service();
    this.s3Artifacts = Collections.synchronizedSet(new LinkedHashSet<S3Artifact>());
    this.prefix = config.getPrefix();
    this.outputFile = config.getOutputFile();
    this.log = log;
  }

  @Override
  public void upload(UploadConfiguration config, LocalArtifact artifact) throws MojoExecutionException, MojoFailureException {
    String file = artifact.getTargetPath().toString();
    boolean isUnresolvedSnapshot = file.toUpperCase().endsWith("-SNAPSHOT.JAR");

    final String s3Key;
    if (isUnresolvedSnapshot) {
      if (config.isAllowUnresolvedSnapshots()) {
        long timestamp = System.currentTimeMillis();
        String start = file.substring(0, file.length() - ".JAR".length());
        String end = file.substring(file.length() - ".JAR".length());
        s3Key = Paths.get(config.getS3ArtifactRoot()).resolve(start + "-" + timestamp + end).toString();
      } else {
        throw new MojoExecutionException("Encountered unresolved snapshot: " + file);
      }
    } else {
      s3Key = Paths.get(config.getS3ArtifactRoot()).resolve(file).toString();
    }

    Path localPath = artifact.getLocalPath();
    if (keyExists(config.getS3Bucket(), s3Key)) {
      log.info("Key already exists " + s3Key);
    } else {
      doUpload(config.getS3Bucket(), s3Key, localPath);
      log.info("Successfully uploaded key " + s3Key);
    }

    String targetPath = prefix.resolve(artifact.getTargetPath()).toString();
    s3Artifacts.add(new S3Artifact(config.getS3Bucket(), s3Key, targetPath, FileHelper.md5(localPath), FileHelper.size(localPath)));
  }

  @Override
  public void destroy() throws MojoFailureException {
    try {
      JsonHelper.writeArtifactsToJson(outputFile.toFile(), new S3ArtifactWrapper(prefix.toString(), s3Artifacts));
    } catch (IOException e) {
      throw new MojoFailureException("Error writing dependencies json to file", e);
    }

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

  private void doUpload(String bucket, String key, Path path) throws MojoFailureException, MojoExecutionException {
    S3Object s3Object = new S3Object(key);
    s3Object.setDataInputFile(path.toFile());
    try {
      s3Object.setContentLength(java.nio.file.Files.size(path));
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading file at path: " + path, e);
    }

    try {
      s3Service.putObject(bucket, s3Object);
    } catch (ServiceException e) {
      throw new MojoFailureException("Error uploading file " + path, e);
    }
  }
}
