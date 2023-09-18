package com.hubspot.maven.plugins.slimfast;

import org.apache.maven.configuration.BeanConfigurator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@Mojo(name = "write-manifest", threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class WriteManifestMojo extends AbstractMojo {
    @Component
    private BeanConfigurator beanConfigurator;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "slimfast.plugin.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "slimfast.outputFile", defaultValue = "${project.build.directory}/slimfast-local.json")
    private String outputFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping plugin execution");
            return;
        }

        LocalArtifactWrapper artifactWrapper = ArtifactHelper.getArtifactPaths(beanConfigurator, project);
        Path outputFile = Paths.get(this.outputFile);
        FileHelper.ensureDirectoryExists(outputFile.getParent());
        Path prefix = artifactWrapper.getPrefix();

        Set<PreparedArtifact> s3Artifacts = new HashSet<>();
        for (LocalArtifact artifact : artifactWrapper.getArtifacts()) {
            s3Artifacts.add(prepareArtifact(artifact));
        }

        PreparedArtifactWrapper preparedArtifactWrapper = new PreparedArtifactWrapper(prefix.toString(), s3Artifacts);
        try {
            JsonHelper.writeArtifactsToJson(outputFile.toFile(), preparedArtifactWrapper);
        } catch (IOException e) {
            throw new MojoFailureException("Failed writing manifest file to disk", e);
        }
    }

    private PreparedArtifact prepareArtifact(LocalArtifact artifact) {
        return new PreparedArtifact(artifact.getLocalPath().toString(), artifact.getTargetPath().toString());
    }
}
