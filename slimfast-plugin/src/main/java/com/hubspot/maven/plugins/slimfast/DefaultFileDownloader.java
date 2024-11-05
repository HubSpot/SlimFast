package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;

public class DefaultFileDownloader implements FileDownloader {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultFileDownloader.class);

  private S3AsyncClient s3AsyncClient;
  private S3TransferManager s3TransferManager;

  private DownloadConfiguration config;

  @Override
  public void init(DownloadConfiguration config) {
    this.config = config;

    this.s3AsyncClient =
      S3Factory.createS3AsyncClient(config.getS3AccessKey(), config.getS3SecretKey());
    this.s3TransferManager = S3Factory.createTransferManager(s3AsyncClient);
  }

  @Override
  public void download(Set<S3Artifact> artifacts)
    throws MojoExecutionException, MojoFailureException {
    try {
      artifacts
        .stream()
        .map(this::fetchCachedFile)
        .collect(DefaultFileUploader.futuresToSet())
        .get(5, TimeUnit.MINUTES)
        .stream()
        .filter(cachedFile -> {
          if (Files.exists(cachedFile.targetFile)) {
            LOG.info("Target file exists {}", cachedFile.targetFile);
            return false;
          }
          return true;
        })
        .forEach(cachedFile -> {
          try {
            FileHelper.ensureDirectoryExists(cachedFile.targetFile.getParent());
            Files.copy(cachedFile.cacheFile, cachedFile.targetFile);
            verifyChecksums(cachedFile.targetFile, cachedFile.artifact);
          } catch (IOException e) {
            throw new UncheckedIOException(
              String.format(
                "Error copying file from %s to %s",
                cachedFile.cacheFile,
                cachedFile.targetFile
              ),
              e
            );
          }
        });
    } catch (Exception e) {
      throw new MojoExecutionException(e);
    }
  }

  private record CachedFile(S3Artifact artifact, Path cacheFile, Path targetFile) {}

  private CompletableFuture<CachedFile> fetchCachedFile(S3Artifact artifact) {
    Path cacheFile = config
      .getCacheDirectory()
      .resolve(config.getPrefix().relativize(Paths.get(artifact.getTargetPath())));
    Path targetFile = config.getOutputDirectory().resolve(artifact.getTargetPath());

    CachedFile result = new CachedFile(artifact, cacheFile, targetFile);

    if (artifactIsCached(cacheFile, artifact)) {
      LOG.info("Target file is cached {}", cacheFile);
      return CompletableFuture.completedFuture(result);
    } else {
      Path tempPath = createTempFile(cacheFile);
      FileHelper.ensureDirectoryExists(cacheFile.getParent());

      return s3TransferManager
        .downloadFile(
          DownloadFileRequest
            .builder()
            .getObjectRequest(req ->
              req.bucket(artifact.getBucket()).key(artifact.getKey())
            )
            .destination(tempPath)
            .build()
        )
        .completionFuture()
        .thenApply(ignored -> {
          FileHelper.atomicMove(tempPath, cacheFile);
          LOG.info("Successfully downloaded key {}", artifact.getKey());
          return result;
        });
    }
  }

  @Override
  public void close() throws IOException {
    s3AsyncClient.close();
  }

  private boolean artifactIsCached(Path path, S3Artifact artifact) {
    return Files.exists(path) && checksumsMatch(path, artifact);
  }

  private Path createTempFile(Path path) {
    try {
      return Files.createTempFile(
        config.getCacheDirectory(),
        String.valueOf(path.getFileName()),
        null
      );
    } catch (IOException e) {
      throw new UncheckedIOException(
        "Error creating temp file in " + config.getCacheDirectory(),
        e
      );
    }
  }

  private static void verifyChecksums(Path path, S3Artifact artifact) {
    long actualSize = FileHelper.size(path);
    long expectedSize = artifact.getSize();
    if (actualSize != expectedSize) {
      throw new IllegalStateException(
        String.format(
          "File %s has unexpected size, expected=%s, actual=%s",
          path,
          expectedSize,
          actualSize
        )
      );
    }

    String actualMd5 = FileHelper.md5(path);
    String expectedMd5 = artifact.getMd5();
    if (!actualMd5.equals(expectedMd5)) {
      throw new IllegalStateException(
        String.format(
          "File %s has unexpected checksum, expected=%s, actual=%s",
          path,
          expectedMd5,
          actualMd5
        )
      );
    }
  }

  private static boolean checksumsMatch(Path path, S3Artifact artifact) {
    return (
      FileHelper.size(path) == artifact.getSize() &&
      FileHelper.md5(path).equals(artifact.getMd5())
    );
  }
}
