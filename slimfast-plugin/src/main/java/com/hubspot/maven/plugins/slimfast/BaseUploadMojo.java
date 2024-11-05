package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

public abstract class BaseUploadMojo extends AbstractMojo {

  private static final Logger LOG = LoggerFactory.getLogger(BaseUploadMojo.class);

  private static final String DEFAULT_UPLOADER =
    "com.hubspot.maven.plugins.slimfast.DefaultFileUploader";
  private static final String DRY_RUN_UPLOADER =
    "com.hubspot.maven.plugins.slimfast.DryRunFileUploader";

  @Parameter(
    property = "slimfast.fileUploader",
    alias = "fileUploader",
    defaultValue = DEFAULT_UPLOADER
  )
  private String fileUploaderType;

  @Parameter(
    property = "slimfast.s3.accessKey",
    defaultValue = "${s3.access.key}",
    required = true
  )
  private String s3AccessKey;

  @Parameter(
    property = "slimfast.s3.secretKey",
    defaultValue = "${s3.secret.key}",
    required = true
  )
  private String s3SecretKey;

  @Parameter(property = "slimfast.s3.region")
  private String s3Region;

  @Parameter(property = "slimfast.dryRun", defaultValue = "false")
  private boolean dryRun;

  @Parameter(
    property = "slimfast.s3.bucket",
    defaultValue = "${s3.bucket}",
    required = true
  )
  private String s3Bucket;

  @Parameter(
    property = "slimfast.s3.artifactPrefix",
    defaultValue = "${s3.artifact.root}",
    required = true
  )
  private String s3ArtifactRoot;

  @Parameter(property = "slimfast.plugin.skip", defaultValue = "false")
  private boolean skip;

  @Parameter(
    property = "slimfast.outputFile",
    defaultValue = "${project.build.directory}/slimfast.json"
  )
  private String outputFile;

  @Parameter(property = "slimfast.allowUnresolvedSnapshots", defaultValue = "false")
  private boolean allowUnresolvedSnapshots;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      LOG.info("Skipping plugin execution");
      return;
    }

    final ArtifactWrapper artifactWrapper = getArtifactWrapper();
    final UploadConfiguration configuration = buildConfiguration(
      artifactWrapper.getPrefix()
    );

    FileHelper.ensureDirectoryExists(configuration.getOutputFile().getParent());

    try (FileUploader fileUploader = instantiateFileUploader()) {
      fileUploader.init(configuration);
      fileUploader.upload(artifactWrapper.getLocalArtifacts());
    } catch (IOException e) {
      throw new MojoExecutionException(e);
    }
  }

  protected abstract ArtifactWrapper getArtifactWrapper() throws MojoExecutionException;

  protected UploadConfiguration buildConfiguration(Path prefix) {
    S3Configuration s3Configuration = new S3Configuration(
      s3AccessKey,
      s3SecretKey,
      Optional.ofNullable(s3Region).map(Region::of),
      Optional.of(20.0), // aws-sdk default is 10.0
      Optional.empty() // aws-sdk default is 8mb
    );

    return new UploadConfiguration(
      s3Configuration,
      prefix,
      s3Bucket,
      s3ArtifactRoot,
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
        throw new MojoExecutionException(
          "May not specify custom fileUploader when using the dryRun flag"
        );
      }
    } else {
      resolvedFileUploaderType = fileUploaderType;
    }

    try {
      return (FileUploader) Class
        .forName(resolvedFileUploaderType)
        .getDeclaredConstructor()
        .newInstance();
    } catch (ClassNotFoundException e) {
      throw new MojoExecutionException("Unable to find file uploader implementation", e);
    } catch (
      InstantiationException
      | IllegalAccessException
      | NoSuchMethodException
      | InvocationTargetException e
    ) {
      throw new MojoExecutionException("Unable to instantiate file uploader", e);
    } catch (ClassCastException e) {
      throw new MojoExecutionException("Must implement FileUploader interface", e);
    }
  }
}
