package com.hubspot.maven.plugins.slimfast;

import java.nio.file.Path;
import java.util.Set;

public class ClasspathConfiguration {

  private final Path prefix;
  private final Set<String> entries;

  public ClasspathConfiguration(Path prefix, Set<String> entries) {
    this.prefix = prefix;
    this.entries = entries;
  }

  public Path getPrefix() {
    return prefix;
  }

  public Set<String> getEntries() {
    return entries;
  }
}
