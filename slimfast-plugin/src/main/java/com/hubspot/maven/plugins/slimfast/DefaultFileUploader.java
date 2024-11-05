package com.hubspot.maven.plugins.slimfast;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

public class DefaultFileUploader extends BaseFileUploader {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultFileUploader.class);

  private S3AsyncClient s3AsyncClient;
  private S3TransferManager s3TransferManager;

  @Override
  public void init(UploadConfiguration config) {
    super.init(config);
    this.s3AsyncClient =
      S3Factory.createS3AsyncClient(config.getS3AccessKey(), config.getS3SecretKey());
    this.s3TransferManager = S3Factory.createTransferManager(s3AsyncClient);
  }

  @Override
  protected void doUpload(Set<S3Artifact> artifacts)
    throws ExecutionException, InterruptedException, TimeoutException {
    Optional<String> bucket = artifacts.stream().findFirst().map(S3Artifact::getBucket);
    if (bucket.isEmpty()) {
      return;
    }

    Set<String> existingKeys = checkForExistingKeys(
      bucket.get(),
      artifacts.stream().map(S3Artifact::getKey).collect(Collectors.toSet())
    );

    artifacts
      .stream()
      .filter(artifact -> {
        if (existingKeys.contains(artifact.getKey())) {
          LOG.info("Key already exists {}", artifact.getKey());
          return false;
        } else {
          return true;
        }
      })
      .map(artifact ->
        s3TransferManager
          .uploadFile(
            UploadFileRequest
              .builder()
              .putObjectRequest(b -> b.bucket(artifact.getBucket()).key(artifact.getKey())
              )
              .source(
                artifact
                  .getLocalPath()
                  .orElseThrow(() ->
                    new IllegalArgumentException(
                      "Artifact " + artifact.getKey() + " is missing localPath"
                    )
                  )
              )
              .build()
          )
          .completionFuture()
          .handle((result, ex) -> {
            if (ex != null) {
              throw new RuntimeException(
                "Error uploading file " + artifact.getLocalPath().get(),
                ex
              );
            }
            LOG.info("Successfully uploaded key {}", artifact.getKey());
            return result;
          })
      )
      .collect(futuresToSet())
      .get(5, TimeUnit.MINUTES);
  }

  @Override
  public void close() {
    s3AsyncClient.close();
  }

  private Set<String> checkForExistingKeys(String bucket, Set<String> keys)
    throws ExecutionException, InterruptedException, TimeoutException {
    return keys
      .stream()
      .map(key ->
        s3AsyncClient
          .headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
          .handle((response, ex) -> {
            if (ex instanceof NoSuchKeyException) {
              return Optional.empty();
            } else if (ex != null) {
              throw new RuntimeException(
                "Error getting object metadata for key: " + key,
                ex
              );
            } else {
              return Optional.of(key);
            }
          })
      )
      .collect(futuresToSet())
      .get(5, TimeUnit.MINUTES)
      .stream()
      .flatMap(Optional::stream)
      .map(key -> (String) key)
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static <T> Collector<CompletableFuture<T>, ?, CompletableFuture<Set<T>>> futuresToSet() {
    return Collectors.collectingAndThen(
      Collectors.toCollection(LinkedHashSet::new),
      futures ->
        CompletableFuture
          .allOf(futures.toArray(new CompletableFuture[0]))
          .thenApply(ignored ->
            futures
              .stream()
              .map(CompletableFuture::join)
              .collect(Collectors.toCollection(LinkedHashSet::new))
          )
    );
  }
}
