package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.maven.configuration.BeanConfigurator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "copy", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class CopyJarsMojo extends AbstractMojo {

  @Component
  private BeanConfigurator beanConfigurator;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter(property = "slimfast.repositoryPath", defaultValue = "${settings.localRepository}")
  private String repositoryPath;

  @Parameter(property = "slimfast.outputDirectory", defaultValue = "${project.build.directory}")
  private String outputDirectory;

  @Parameter(property = "slimfast.plugin.skip", defaultValue = "false")
  private boolean skip;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    ClasspathConfiguration classpath = ManifestHelper.getClasspathConfiguration(beanConfigurator, project, session);
    Path classpathPrefix = classpath.getPrefix();
    ArtifactLocator artifactLocator = new ArtifactLocator(project, repositoryPath);

    for (String classpathEntry : classpath.getEntries()) {
      Path localPath = artifactLocator.locateClasspathEntry(classpathEntry);
      Path targetPath = Paths.get(outputDirectory).resolve(classpathPrefix).resolve(classpathEntry);
      FileHelper.ensureDirectoryExists(targetPath.getParent());

      try {
        Files.copy(localPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        throw new MojoFailureException(String.format("Error moving file from %s to %s", localPath, targetPath), e);
      }
    }
  }
}
