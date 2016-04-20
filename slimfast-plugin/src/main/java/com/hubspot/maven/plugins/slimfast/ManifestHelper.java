package com.hubspot.maven.plugins.slimfast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.archiver.ManifestConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

public class ManifestHelper {

  public static Set<String> getClasspathEntries(ManifestConfiguration manifestConfiguration,
                                                MavenProject project,
                                                MavenSession session) throws MojoExecutionException {
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
}
