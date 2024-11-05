package com.hubspot.maven.plugins.slimfast;

import java.io.IOException;
import java.nio.file.Paths;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
  name = "upload-from-manifest",
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class UploadFromManifestMojo extends BaseUploadMojo {

  @Parameter(
    property = "slimfast.manifestFile",
    defaultValue = "${project.build.directory}/slimfast-local.json"
  )
  private String manifestFile;

  @Override
  protected ArtifactWrapper getArtifactWrapper() throws MojoExecutionException {
    try {
      return JsonHelper.readPreparedArtifactsFromJson(Paths.get(manifestFile).toFile());
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to read manifest file", e);
    }
  }
}
