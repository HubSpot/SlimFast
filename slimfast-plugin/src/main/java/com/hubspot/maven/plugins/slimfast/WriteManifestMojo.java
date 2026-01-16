package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(
  name = "write-manifest",
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class WriteManifestMojo extends AbstractMojo {

  private static final Logger LOG = LoggerFactory.getLogger(WriteManifestMojo.class);

  @Inject
  private ArtifactHelper artifactHelper;

  @Parameter(property = "slimfast.plugin.skip", defaultValue = "false")
  private boolean skip;

  @Parameter(
    property = "slimfast.outputFile",
    defaultValue = "${project.build.directory}/slimfast-local.json"
  )
  private String outputFile;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    LocalArtifactWrapper artifactWrapper = artifactHelper.getArtifactPaths();
    Path outputFile = Paths.get(this.outputFile);
    FileHelper.ensureDirectoryExists(outputFile.getParent());
    Path prefix = artifactWrapper.getPrefix();

    Set<PreparedArtifact> s3Artifacts = new HashSet<>();

    for (LocalArtifact artifact : artifactWrapper.getArtifacts()) {
      s3Artifacts.add(prepareArtifact(artifact));
    }

    if (s3Artifacts.isEmpty()) {
      LOG.error("s3Artifacts is empty for {}", outputFile.getParent());
    }

    PreparedArtifactWrapper preparedArtifactWrapper = new PreparedArtifactWrapper(
      prefix,
      s3Artifacts
    );
    try {
      if (preparedArtifactWrapper.getArtifacts().isEmpty()) {
        LOG.error("preparedArtifactWrapper is empty for {}", outputFile.getParent());
      }
      JsonHelper.writeArtifactsToJson(outputFile.toFile(), preparedArtifactWrapper);
    } catch (IOException e) {
      throw new MojoFailureException("Failed writing manifest file to disk", e);
    }
  }

  private PreparedArtifact prepareArtifact(LocalArtifact artifact) {
    return new PreparedArtifact(
      artifact.getLocalPath().toString(),
      artifact.getTargetPath().toString()
    );
  }
}
