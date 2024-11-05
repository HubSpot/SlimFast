package com.hubspot.maven.plugins.slimfast;

import static org.apache.maven.archiver.MavenArchiver.REPOSITORY_LAYOUT;
import static org.apache.maven.archiver.MavenArchiver.REPOSITORY_LAYOUT_NONUNIQUE;
import static org.apache.maven.archiver.MavenArchiver.SIMPLE_LAYOUT;
import static org.apache.maven.archiver.MavenArchiver.SIMPLE_LAYOUT_NONUNIQUE;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.archiver.ManifestConfiguration;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.configuration.BeanConfigurationException;
import org.apache.maven.configuration.BeanConfigurationRequest;
import org.apache.maven.configuration.BeanConfigurator;
import org.apache.maven.configuration.DefaultBeanConfigurationRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;

@Named
@Singleton
public class ArtifactHelper {

  private static final List<String> ARTIFACT_EXPRESSION_PREFIXES =
    Collections.singletonList("artifact.");

  private final BeanConfigurator beanConfigurator;
  private final MavenProject project;

  @Inject
  public ArtifactHelper(BeanConfigurator beanConfigurator, MavenProject project) {
    this.beanConfigurator = beanConfigurator;
    this.project = project;
  }

  public LocalArtifactWrapper getArtifactPaths() throws MojoExecutionException {
    ManifestConfiguration manifestConfiguration = parseManifestConfiguration(
      beanConfigurator,
      project
    );

    if (!manifestConfiguration.isAddClasspath()) {
      throw new MojoExecutionException(
        "maven-jar-plugin is not configured to add classpath"
      );
    }

    Set<LocalArtifact> artifacts = new HashSet<>();
    for (String classpathElement : classpathElements(project)) {
      File classpathFile = new File(classpathElement);
      if (classpathFile.getAbsoluteFile().isFile()) {
        Artifact artifact = findArtifactWithFile(project.getArtifacts(), classpathFile);

        Path localPath = classpathFile.toPath();
        final Path targetPath;
        if (artifact == null || manifestConfiguration.getClasspathLayoutType() == null) {
          targetPath = localPath.getFileName();
        } else {
          targetPath = computePath(artifact, manifestConfiguration);
        }

        artifacts.add(new LocalArtifact(localPath, targetPath));
      }
    }

    Path prefix = Paths.get("target").resolve(manifestConfiguration.getClasspathPrefix());
    return new LocalArtifactWrapper(prefix, artifacts);
  }

  private static ManifestConfiguration parseManifestConfiguration(
    BeanConfigurator beanConfigurator,
    MavenProject project
  ) throws MojoExecutionException {
    MavenArchiveConfiguration archiveConfiguration = new MavenArchiveConfiguration();

    BeanConfigurationRequest beanConfiguration = new DefaultBeanConfigurationRequest()
      .setBean(archiveConfiguration)
      .setConfiguration(
        project.getModel(),
        "org.apache.maven.plugins",
        "maven-jar-plugin",
        "default-jar"
      );

    beanConfiguration.setConfiguration(beanConfiguration.getConfiguration(), "archive");

    try {
      beanConfigurator.configureBean(beanConfiguration);
    } catch (BeanConfigurationException e) {
      throw new MojoExecutionException("Error parsing archive configuration", e);
    }

    return archiveConfiguration.getManifest();
  }

  private static List<String> classpathElements(MavenProject project)
    throws MojoExecutionException {
    try {
      return project.getRuntimeClasspathElements();
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Error resolving dependencies", e);
    }
  }

  private static Path computePath(Artifact artifact, ManifestConfiguration config)
    throws MojoExecutionException {
    String layoutType = config.getClasspathLayoutType();
    String layout = config.getCustomClasspathLayout();

    Interpolator interpolator = new StringSearchInterpolator();

    List<ValueSource> valueSources = new ArrayList<>();

    valueSources.add(
      new PrefixedObjectValueSource(ARTIFACT_EXPRESSION_PREFIXES, artifact, true)
    );
    valueSources.add(
      new PrefixedObjectValueSource(
        ARTIFACT_EXPRESSION_PREFIXES,
        artifact.getArtifactHandler(),
        true
      )
    );

    Properties extraExpressions = new Properties();
    if (!artifact.isSnapshot()) {
      extraExpressions.setProperty("baseVersion", artifact.getVersion());
    }

    extraExpressions.setProperty("groupIdPath", artifact.getGroupId().replace('.', '/'));
    if (artifact.hasClassifier()) {
      extraExpressions.setProperty("dashClassifier", "-" + artifact.getClassifier());
      extraExpressions.setProperty("dashClassifier?", "-" + artifact.getClassifier());
    } else {
      extraExpressions.setProperty("dashClassifier", "");
      extraExpressions.setProperty("dashClassifier?", "");
    }
    valueSources.add(
      new PrefixedPropertiesValueSource(
        ARTIFACT_EXPRESSION_PREFIXES,
        extraExpressions,
        true
      )
    );

    for (ValueSource vs : valueSources) {
      interpolator.addValueSource(vs);
    }

    RecursionInterceptor recursionInterceptor = new PrefixAwareRecursionInterceptor(
      ARTIFACT_EXPRESSION_PREFIXES
    );

    try {
      boolean useUniqueVersionsLayout = config.isUseUniqueVersions();

      final String resolvedLayout;
      switch (layoutType) {
        case ManifestConfiguration.CLASSPATH_LAYOUT_TYPE_SIMPLE:
          resolvedLayout =
            useUniqueVersionsLayout ? SIMPLE_LAYOUT : SIMPLE_LAYOUT_NONUNIQUE;
          break;
        case ManifestConfiguration.CLASSPATH_LAYOUT_TYPE_REPOSITORY:
          resolvedLayout =
            useUniqueVersionsLayout ? REPOSITORY_LAYOUT : REPOSITORY_LAYOUT_NONUNIQUE;
          break;
        case ManifestConfiguration.CLASSPATH_LAYOUT_TYPE_CUSTOM:
          resolvedLayout = layout;
          break;
        default:
          throw new MojoExecutionException(
            "Unknown classpath layout type: " + layoutType
          );
      }

      return Paths.get(interpolator.interpolate(resolvedLayout, recursionInterceptor));
    } catch (InterpolationException e) {
      throw new MojoExecutionException("Error computing path for classpath entry", e);
    }
  }

  private static Artifact findArtifactWithFile(Set<Artifact> artifacts, File file) {
    for (Artifact artifact : artifacts) {
      if (file.equals(artifact.getFile())) {
        return artifact;
      }
    }

    return null;
  }
}
