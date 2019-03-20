package com.hubspot.maven.plugins.slimfast;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

@Mojo(name = "download", requiresProject = false, threadSafe = true)
public class DownloadJarsMojo extends AbstractMojo {

  @Parameter(property = "slimfast.fileDownloader", alias = "fileDownloader", defaultValue = "com.hubspot.maven.plugins.slimfast.DefaultFileDownloader")
  private String fileDownloaderType;

  @Parameter(property = "slimfast.s3.accessKey", defaultValue = "${s3.access.key}", required = true)
  private String s3AccessKey;

  @Parameter(property = "slimfast.s3.secretKey", defaultValue = "${s3.secret.key}", required = true)
  private String s3SecretKey;

  @Parameter(property = "slimfast.s3.endpoint", defaultValue = "${s3.endpoint.key}", required = false)
  private String s3Endpoint;

  @Parameter(property = "slimfast.s3.downloadThreads", defaultValue = "10")
  private int s3DownloadThreads;

  @Parameter(property = "slimfast.cacheDirectory", defaultValue = "${settings.localRepository}")
  private String cacheDirectory;

  @Parameter(property = "slimfast.outputDirectory", defaultValue = "${basedir}")
  private String outputDirectory;

  @Parameter(property = "slimfast.inputFile", defaultValue = "target/slimfast.json")
  private String inputFile;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    S3ArtifactWrapper wrapper = readArtifactInfo();

    final DownloadConfiguration configuration = buildConfiguration(wrapper.getPrefix());
    FileHelper.ensureDirectoryExists(configuration.getCacheDirectory());

    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("slimfast-download").setDaemon(true).build();
    ExecutorService executor = Executors.newFixedThreadPool(s3DownloadThreads, threadFactory);
    final FileDownloader downloader = instantiateFileDownloader();
    downloader.init(configuration, getLog());

    List<Future<?>> futures = new ArrayList<>();
    for (final S3Artifact artifact : wrapper.getArtifacts()) {
      futures.add(executor.submit(new Callable<Object>() {

        @Override
        public Object call() throws Exception {
          downloader.download(configuration, artifact);
          return null;
        }
      }));
    }

    executor.shutdown();
    waitForDownloadsToFinish(executor, futures);
    downloader.destroy();
  }

  private S3ArtifactWrapper readArtifactInfo() throws MojoFailureException {
    try {
      return JsonHelper.readArtifactsFromJson(new File(inputFile));
    } catch (IOException e) {
      throw new MojoFailureException("Error reading dependencies from file", e);
    }
  }

  private void waitForDownloadsToFinish(ExecutorService executor, List<Future<?>> futures) throws MojoExecutionException, MojoFailureException {
    try {
      if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
        getLog().error("Took more than 5 minutes to download files, quitting");
        throw new MojoExecutionException("Took more than 5 minutes to download files");
      }

      for (Future<?> future : futures) {
        future.get();

      }
    } catch (InterruptedException e) {
      throw new MojoExecutionException("Interrupted", e);
    } catch (ExecutionException e) {
      Throwables.propagateIfInstanceOf(e.getCause(), MojoExecutionException.class);
      Throwables.propagateIfInstanceOf(e.getCause(), MojoFailureException.class);
      throw new MojoExecutionException("Unexpected exception", e.getCause());
    }
  }

  private DownloadConfiguration buildConfiguration(String prefix) {
    return new DownloadConfiguration(
        Paths.get(prefix),
        Paths.get(cacheDirectory),
        Paths.get(outputDirectory),
        s3AccessKey,
        s3SecretKey,
        s3Endpoint
    );
  }

  private FileDownloader instantiateFileDownloader() throws MojoExecutionException {
    try {
      return (FileDownloader) Class.forName(fileDownloaderType).newInstance();
    } catch (ClassNotFoundException e) {
      throw new MojoExecutionException("Unable to find file downloader implementation", e);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new MojoExecutionException("Unable to instantiate file downloader", e);
    } catch (ClassCastException e) {
      throw new MojoExecutionException("Must implement FileDownloader interface", e);
    }
  }
}
