package com.github.andirady.pomcli;

import java.util.Optional;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

public interface ResolutionProvider {

    Optional<Dependency> findByArtifactId(Model model, String artifactId, String scope);

}
