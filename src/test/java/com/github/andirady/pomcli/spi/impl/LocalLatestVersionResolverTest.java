package com.github.andirady.pomcli.spi.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

import com.github.andirady.pomcli.spi.LatestVersionResolver;

class LocalLatestVersionResolverTest {

    @Test
    void shouldReturnLatestVersionIfLocallyAvailable() {
        LatestVersionResolver resolver = new LocalLatestVersionResolver();
        var dependency = new Dependency();
        dependency.setGroupId("org.junit.jupiter");
        dependency.setArtifactId("junit-jupiter-api");
        var result = resolver.resolveLatest(dependency);

        assertTrue(result::isPresent);
    }

    @Test
    void shouldReturnEmptyIfNotLocallyAvailable() {
        LatestVersionResolver resolver = new LocalLatestVersionResolver();
        var dependency = new Dependency();
        dependency.setGroupId("com.example");
        dependency.setArtifactId("test-" + UUID.randomUUID());
        var result = resolver.resolveLatest(dependency);

        assertTrue(result::isEmpty);
    }
}
