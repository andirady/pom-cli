package com.github.andirady.pomcli;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

public interface ResolutionProvider {

    static ResolutionProvider getInstance() {
        return ServiceLoader.load(ResolutionProvider.class)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No provider for " + ResolutionProvider.class.getName()));
    }

    Model readModel(String groupId, String artifactId, String version);

    Optional<Dependency> findByArtifactId(Model model, String groupId, String artifactId, String scope);

}
