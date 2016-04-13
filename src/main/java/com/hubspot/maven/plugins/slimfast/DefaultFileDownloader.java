package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3Object;

public class DefaultFileDownloader implements FileDownloader {
  private S3Service s3Service;
  private Path cacheDirectory;
  private Log log;

  @Override
  public void init(DownloadConfiguration config, Log log) {
    this.s3Service = config.newS3Service();
    this.cacheDirectory = config.getCacheDirectory();
    this.log = log;
  }

  @Override
  public void download(DownloadConfiguration config, S3Artifact artifact) throws MojoExecutionException, MojoFailureException {
    Path targetFile = config.getOutputDirectory().resolve(artifact.getTargetPath());
    if (artifactIsCached(targetFile, artifact)) {
      log.info("Target file exists " + targetFile);
      return;
    }

    Path cacheFile = cacheDirectory.resolve(config.getClasspathPrefix().relativize(Paths.get(artifact.getTargetPath())));
    if (artifactIsCached(cacheFile, artifact)) {
      log.info("Target file is cached " + cacheFile);
    } else {
      doDownload(artifact.getBucket(), artifact.getKey(), cacheFile);
      log.info("Successfully downloaded key " + artifact.getKey());
    }

    FileHelper.ensureDirectoryExists(targetFile.getParent());

    try {
      Files.copy(cacheFile, targetFile);
    } catch (IOException e) {
      throw new MojoFailureException(String.format("Error copying file from %s to %s", cacheFile, targetFile), e);
    }

    verifyChecksums(targetFile, artifact);
  }

  @Override
  public void destroy() throws MojoExecutionException, MojoFailureException {
    try {
      s3Service.shutdown();
    } catch (ServiceException e) {
      throw new MojoFailureException("Error closing S3Service", e);
    }
  }

  private boolean artifactIsCached(Path path, S3Artifact artifact) throws MojoExecutionException {
    return Files.exists(path) && checksumsMatch(path, artifact);
  }

  private void doDownload(String bucket, String key, Path path) throws MojoFailureException, MojoExecutionException {
    Path tempPath = createTempFile(path);

    try {
      S3Object s3Object = s3Service.getObject(bucket, key);
      try (InputStream input = s3Object.getDataInputStream()) {
        Files.copy(input, tempPath, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (ServiceException e) {
      throw new MojoFailureException("Error downloading key " + key, e);
    } catch (IOException e) {
      throw new MojoFailureException("Error downloading to file " + tempPath, e);
    }

    try {
      FileHelper.ensureDirectoryExists(path.getParent());
      Files.move(tempPath, path, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new MojoFailureException(String.format("Error moving file from %s to %s", tempPath, path), e);
    }
  }

  private Path createTempFile(Path path) throws MojoFailureException {
    try {
      return Files.createTempFile(cacheDirectory, String.valueOf(path.getFileName()), null);
    } catch (IOException e) {
      throw new MojoFailureException("Error creating temp file in " + cacheDirectory, e);
    }
  }

  private static void verifyChecksums(Path path, S3Artifact artifact) throws MojoExecutionException {
    long actualSize = FileHelper.size(path);
    long expectedSize = artifact.getSize();
    if (actualSize != expectedSize) {
      throw new MojoExecutionException(String.format("File %s has unexpected size, expected=%s, actual=%s", path, expectedSize, actualSize));
    }

    String actualMd5 = FileHelper.md5(path);
    String expectedMd5 = artifact.getMd5();
    if (!actualMd5.equals(expectedMd5)) {
      throw new MojoExecutionException(String.format("File %s has unexpected checksum, expected=%s, actual=%s", path, expectedMd5, actualMd5));
    }
  }

  private static boolean checksumsMatch(Path path, S3Artifact artifact) throws MojoExecutionException {
    return FileHelper.size(path) == artifact.getSize() && FileHelper.md5(path).equals(artifact.getMd5());
  }
}
