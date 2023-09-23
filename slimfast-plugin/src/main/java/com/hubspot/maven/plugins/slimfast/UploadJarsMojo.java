package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
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

import org.apache.maven.configuration.BeanConfigurator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

@Mojo(name = "upload", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class UploadJarsMojo extends AbstractMojo {
  private static final String DEFAULT_UPLOADER = "com.hubspot.maven.plugins.slimfast.DefaultFileUploader";
  private static final String DRY_RUN_UPLOADER = "com.hubspot.maven.plugins.slimfast.DryRunFileUploader";

  @Component
  private BeanConfigurator beanConfigurator;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(property = "slimfast.fileUploader", alias = "fileUploader", defaultValue = DEFAULT_UPLOADER)
  private String fileUploaderType;

  @Parameter(property = "slimfast.s3.bucket", defaultValue = "${s3.bucket}", required = true)
  private String s3Bucket;

  @Parameter(property = "slimfast.s3.artifactPrefix", defaultValue = "${s3.artifact.root}", required = true)
  private String s3ArtifactRoot;

  @Parameter(property = "slimfast.s3.accessKey", defaultValue = "${s3.access.key}")
  private String s3AccessKey;

  @Parameter(property = "slimfast.s3.secretKey", defaultValue = "${s3.secret.key}")
  private String s3SecretKey;

  @Parameter(property = "slimfast.s3.uploadThreads", defaultValue = "10")
  private int s3UploadThreads;

  @Parameter(property = "slimfast.repositoryPath", defaultValue = "${settings.localRepository}")
  private String repositoryPath;

  @Parameter(property = "slimfast.plugin.skip", defaultValue = "false")
  private boolean skip;

  @Parameter(property = "slimfast.outputFile", defaultValue = "${project.build.directory}/slimfast.json")
  private String outputFile;

  @Parameter(property = "slimfast.allowUnresolvedSnapshots", defaultValue = "false")
  private boolean allowUnresolvedSnapshots;

  @Parameter(property = "slimfast.dryRun", defaultValue = "false")
  private boolean dryRun;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    LocalArtifactWrapper artifactWrapper = ArtifactHelper.getArtifactPaths(beanConfigurator, project);
    final UploadConfiguration configuration = buildConfiguration(artifactWrapper.getPrefix());
    FileHelper.ensureDirectoryExists(configuration.getOutputFile().getParent());

    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("slimfast-upload").setDaemon(true).build();
    ExecutorService executor = Executors.newFixedThreadPool(s3UploadThreads, threadFactory);
    final FileUploader uploader = instantiateFileUploader();
    uploader.init(configuration, getLog());

    List<Future<?>> futures = new ArrayList<>();
    for (final LocalArtifact artifact : artifactWrapper.getArtifacts()) {
      futures.add(executor.submit(new Callable<Object>() {

        @Override
        public Object call() throws Exception {
          uploader.upload(configuration, artifact);
          return null;
        }
      }));
    }

    executor.shutdown();
    waitForUploadsToFinish(executor, futures);
    uploader.destroy();
  }

  private void waitForUploadsToFinish(ExecutorService executor, List<Future<?>> futures) throws MojoExecutionException, MojoFailureException {
    try {
      if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
        getLog().error("Took more than 5 minutes to upload files, quitting");
        throw new MojoExecutionException("Took more than 5 minutes to upload files");
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

  private UploadConfiguration buildConfiguration(Path prefix) {
    return new UploadConfiguration(
        prefix,
        s3Bucket,
        s3ArtifactRoot,
        s3AccessKey,
        s3SecretKey,
        Paths.get(outputFile),
        allowUnresolvedSnapshots
    );
  }

  private FileUploader instantiateFileUploader() throws MojoExecutionException {
    final String resolvedFileUploaderType;
    if (dryRun) {
      if (DEFAULT_UPLOADER.equals(fileUploaderType)) {
        resolvedFileUploaderType = DRY_RUN_UPLOADER;
      } else {
        throw new MojoExecutionException("May not specify custom fileUploader when using the dryRun flag");
      }
    } else {
      resolvedFileUploaderType = fileUploaderType;
    }

    try {
      return (FileUploader) Class.forName(resolvedFileUploaderType).newInstance();
    } catch (ClassNotFoundException e) {
      throw new MojoExecutionException("Unable to find file uploader implementation", e);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new MojoExecutionException("Unable to instantiate file uploader", e);
    } catch (ClassCastException e) {
      throw new MojoExecutionException("Must implement FileUploader interface", e);
    }
  }
}
