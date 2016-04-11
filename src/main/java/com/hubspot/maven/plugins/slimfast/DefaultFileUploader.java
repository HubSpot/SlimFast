package com.hubspot.maven.plugins.slimfast;

import com.google.common.io.Files;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.HttpException;
import org.jets3t.service.model.S3Object;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.hash.Hashing;

public class DefaultFileUploader implements FileUploader {
  private S3Service s3Service;
  private JSONArray s3Dependencies;
  private File outputFile;
  private Log log;

  @Override
  public void init(Configuration config, Log log) {
    this.s3Service = config.newS3Service();
    this.s3Dependencies = new JSONArray();
    this.outputFile = new File(config.getOutputFile());
    this.log = log;
  }

  @Override
  public void upload(Configuration config, String file) throws MojoExecutionException, MojoFailureException {
    if (!config.isAllowUnresolvedSnapshots() && file.toUpperCase().endsWith("-SNAPSHOT.JAR")) {
      throw new MojoExecutionException("Encountered unresolved snapshot: " + file);
    }

    String s3Key = Paths.get(config.getS3ArtifactRoot()).resolve(file).toString();
    Path localPath = Paths.get(config.getRepositoryPath()).resolve(file);

    if (keyExists(config.getS3Bucket(), s3Key)) {
      log.info("Key already exists " + s3Key);
    } else {
      doUpload(config.getS3Bucket(), s3Key, localPath);
      log.info("Successfully uploaded key " + s3Key);
    }

    JSONObject s3Dependency = new JSONObject();
    s3Dependency.put("s3Bucket", config.getS3Bucket());
    s3Dependency.put("s3ObjectKey", s3Key);
    s3Dependency.put("targetPath", Paths.get(config.getClasspathPrefix()).resolve(file).toString());
    s3Dependency.put("md5", md5(localPath));
    s3Dependency.put("filesize", size(localPath));
    s3Dependencies.add(s3Dependency);
  }

  @Override
  public void destroy() throws MojoFailureException {
    try {
      writeDependenciesJson();
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

  private void writeDependenciesJson() throws IOException, MojoFailureException {
    try (Writer writer = newWriter(outputFile)) {
      s3Dependencies.writeJSONString(writer);
      writer.flush();
    }
  }

  private static Writer newWriter(File file) throws IOException {
    FileOutputStream outputStream = new FileOutputStream(file);
    return new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
  }

  private static String md5(Path path) throws MojoExecutionException {
    try {
      return Files.hash(path.toFile(), Hashing.md5()).toString();
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading file at path: " + path, e);
    }
  }

  private static long size(Path path) throws MojoExecutionException {
    try {
      return java.nio.file.Files.size(path);
    } catch (IOException e) {
      throw new MojoExecutionException("Error reading file at path: " + path, e);
    }
  }
}
