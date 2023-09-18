package com.hubspot.maven.plugins.slimfast;

import java.util.Set;

public class PreparedArtifactWrapper {
    private final String prefix;
    private final Set<PreparedArtifact> artifacts;

    public PreparedArtifactWrapper(String prefix, Set<PreparedArtifact> artifacts) {
        this.prefix = prefix;
        this.artifacts = artifacts;
    }

    public String getPrefix() {
        return prefix;
    }

    public Set<PreparedArtifact> getArtifacts() {
        return artifacts;
    }
}
