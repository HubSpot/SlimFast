package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.project.MavenProject;

public class ArtifactLocator {
  private final Map<String, Artifact> reactorArtifacts;
  private final Path localRepository;

  public ArtifactLocator(MavenProject project, String localRepository) {
    this.reactorArtifacts = findReactorArtifacts(project, localRepository);
    this.localRepository = Paths.get(localRepository);
  }

  public Path locateClasspathEntry(String classpathEntry) {
    if (reactorArtifacts.containsKey(classpathEntry)) {
      return reactorArtifacts.get(classpathEntry).getFile().toPath();
    } else {
      return localRepository.resolve(classpathEntry);
    }
  }

  private static Map<String, Artifact> findReactorArtifacts(MavenProject project, String localRepository) {
    Map<String, Artifact> reactorArtifacts = new HashMap<>();

    for (Artifact artifact : project.getArtifacts()) {
      if (artifact.getFile() != null && !artifact.getFile().toPath().startsWith(Paths.get(localRepository))) {
        reactorArtifacts.put(new DefaultRepositoryLayout().pathOf(artifact), artifact);
      }
    }

    return reactorArtifacts;
  }
}
