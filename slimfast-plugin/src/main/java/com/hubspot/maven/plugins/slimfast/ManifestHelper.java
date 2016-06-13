package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.archiver.ManifestConfiguration;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.configuration.BeanConfigurationException;
import org.apache.maven.configuration.BeanConfigurationRequest;
import org.apache.maven.configuration.BeanConfigurator;
import org.apache.maven.configuration.DefaultBeanConfigurationRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

public class ManifestHelper {

  public static ClasspathConfiguration getClasspathConfiguration(BeanConfigurator beanConfigurator,
                                                                 MavenProject project,
                                                                 MavenSession session) throws MojoExecutionException {
    ManifestConfiguration manifestConfiguration = parseManifestConfiguration(beanConfigurator, project);

    if (!manifestConfiguration.isAddClasspath()) {
      throw new MojoExecutionException("maven-jar-plugin is not configured to add classpath");
    }

    Path classpathPrefix = Paths.get(manifestConfiguration.getClasspathPrefix());
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

    return new ClasspathConfiguration(classpathPrefix, classpathEntries);
  }

  private static ManifestConfiguration parseManifestConfiguration(BeanConfigurator beanConfigurator,
                                                           MavenProject project) throws MojoExecutionException {
    MavenArchiveConfiguration archiveConfiguration = new MavenArchiveConfiguration();

    BeanConfigurationRequest beanConfiguration = new DefaultBeanConfigurationRequest()
        .setBean(archiveConfiguration)
        .setConfiguration(project.getModel(), "org.apache.maven.plugins", "maven-jar-plugin", "default-jar");

    beanConfiguration.setConfiguration(beanConfiguration.getConfiguration(), "archive");

    try {
      beanConfigurator.configureBean(beanConfiguration);
    } catch (BeanConfigurationException e) {
      throw new MojoExecutionException("Error parsing archive configuration", e);
    }

    return archiveConfiguration.getManifest();
  }
}
