package com.github.andirady.pomcli.spi;

import java.util.Comparator;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.maven.model.Dependency;

public interface LatestVersionResolver {

    /**
     * Resolve the latest version from the first available provider.
     */
    static Optional<String> resolve(Dependency dependency) {
        return ServiceLoader.load(LatestVersionResolver.class).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparingInt(LatestVersionResolver::priority))
                .filter(LatestVersionResolver::available)
                .findFirst()
                .flatMap(it -> it.resolveLatest(dependency));
    }

    /**
     * The priority of the provider
     */
    int priority();

    /**
     * Returns true if the provider is available, false otherwise.
     *
     * NOTE: Implementation should not spend too much time here.
     */
    boolean available();

    /**
     * Resolve the latest version for the specified the dependency.
     *
     * @param dependency the dependency to resolve.
     */
    Optional<String> resolveLatest(Dependency dependency);

}
