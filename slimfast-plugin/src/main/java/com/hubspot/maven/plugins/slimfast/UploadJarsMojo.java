package com.hubspot.maven.plugins.slimfast;

import javax.inject.Inject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
  name = "upload",
  defaultPhase = LifecyclePhase.DEPLOY,
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class UploadJarsMojo extends BaseUploadMojo {

  @Inject
  private ArtifactHelper artifactHelper;

  @Override
  protected ArtifactWrapper getArtifactWrapper() throws MojoExecutionException {
    return artifactHelper.getArtifactPaths();
  }
}
