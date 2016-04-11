package com.hubspot.maven.plugins.slimfast;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.maven.archiver.ManifestConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Mojo(name = "upload", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class UploadJarsMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter(alias = "manifest")
  private ManifestConfiguration manifestConfiguration = new ManifestConfiguration();

  @Parameter(alias = "fileUploader", defaultValue = "com.hubspot.maven.plugins.slimfast.DefaultFileUploader")
  private String fileUploaderType;

  @Parameter(defaultValue = "${s3.bucket}", required = true)
  private String s3Bucket;

  @Parameter(defaultValue = "${s3.artifact.root}", required = true)
  private String s3ArtifactRoot;

  @Parameter(defaultValue = "${s3.access.key}", required = true)
  private String s3AccessKey;

  @Parameter(defaultValue = "${s3.secret.key}", required = true)
  private String s3SecretKey;

  @Parameter(defaultValue = "10")
  private int s3UploadThreads;

  @Parameter(defaultValue = "${settings.localRepository}")
  private String repositoryPath;

  @Parameter(property = "slimfast.plugin.skip", defaultValue = "false")
  private boolean skip;

  @Parameter(defaultValue = "${project.build.directory}/s3.dependencies.json")
  private String outputFile;

  @Parameter(defaultValue = "false")
  private boolean allowUnresolvedSnapshots;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    final Configuration configuration = buildConfiguration();
    Set<String> classpathEntries = getClasspathEntries();

    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("slimfast-upload").setDaemon(true).build();
    ExecutorService executor = Executors.newFixedThreadPool(s3UploadThreads, threadFactory);
    final FileUploader uploader = instantiateFileUploader();
    uploader.init(configuration, getLog());

    List<Future<?>> futures = new ArrayList<>();
    for (final String classpathEntry : classpathEntries) {
      futures.add(executor.submit(new Callable<Object>() {

        @Override
        public Object call() throws Exception {
          uploader.upload(configuration, classpathEntry);
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

  private Configuration buildConfiguration() {
    return new Configuration(
        manifestConfiguration.getClasspathPrefix(),
        repositoryPath,
        s3Bucket,
        s3ArtifactRoot,
        s3AccessKey,
        s3SecretKey,
        outputFile,
        allowUnresolvedSnapshots
    );
  }

  private Set<String> getClasspathEntries() throws MojoExecutionException {
    manifestConfiguration.setAddClasspath(true);
    manifestConfiguration.setClasspathPrefix("");

    final Manifest manifest;
    try {
      MavenArchiver archiver = new MavenArchiver();
      manifest = archiver.getManifest(session, project, manifestConfiguration);
    } catch (ManifestException | DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Error building manifest", e);
    }

    Set<String> classpathEntries = new HashSet<>();
    String classpath = manifest.getMainAttributes().getValue("Class-Path");
    classpathEntries.addAll(Arrays.asList(classpath.split(" ")));

    return classpathEntries;
  }

  private FileUploader instantiateFileUploader() throws MojoExecutionException {
    try {
      return (FileUploader) Class.forName(fileUploaderType).newInstance();
    } catch (ClassNotFoundException e) {
      throw new MojoExecutionException("Unable to find file uploader implementation", e);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new MojoExecutionException("Unable to instantiate file uploader", e);
    } catch (ClassCastException e) {
      throw new MojoExecutionException("Must implement FileUploader interface", e);
    }
  }
}
