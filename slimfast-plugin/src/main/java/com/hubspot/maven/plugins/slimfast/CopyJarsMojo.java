package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
  name = "copy",
  defaultPhase = LifecyclePhase.PACKAGE,
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class CopyJarsMojo extends AbstractMojo {

  @Inject
  private ArtifactHelper artifactHelper;

  @Parameter(property = "slimfast.outputDirectory", defaultValue = "${basedir}")
  private String outputDirectory;

  @Parameter(property = "slimfast.plugin.skip", defaultValue = "false")
  private boolean skip;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    LocalArtifactWrapper artifactWrapper = artifactHelper.getArtifactPaths();
    Path prefix = artifactWrapper.getPrefix();

    for (LocalArtifact artifact : artifactWrapper.getArtifacts()) {
      Path localPath = artifact.getLocalPath();
      Path targetPath = Paths
        .get(outputDirectory)
        .resolve(prefix)
        .resolve(artifact.getTargetPath());
      FileHelper.ensureDirectoryExists(targetPath.getParent());

      try {
        Files.copy(localPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        throw new MojoFailureException(
          String.format("Error moving file from %s to %s", localPath, targetPath),
          e
        );
      }
    }
  }
}
