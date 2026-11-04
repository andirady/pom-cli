package com.github.andirady.pomcli.spi.impl;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.reducing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.apache.maven.model.Dependency;

import com.github.andirady.pomcli.spi.LatestVersionResolver;

class LocalLatestVersionResolver implements LatestVersionResolver {

    private final Path mvnRepo;

    LocalLatestVersionResolver() {
        mvnRepo = Path.of(System.getProperty("user.home"), ".m2", "repository");
    }

    @Override
    public int priority() {
        return 99; // Least priority.
    }

    @Override
    public boolean available() {
        return Files.isDirectory(mvnRepo);
    }

    @Override
    public Optional<String> resolveLatest(Dependency dependency) {
        var localPath = Arrays.stream(dependency.getGroupId().split("\\."))
                .map(Path::of)
                .collect(collectingAndThen(
                        reducing(mvnRepo, Path::resolve),
                        p -> p.resolve(dependency.getArtifactId())));

        try {
            return Files.list(localPath).filter(Files::isDirectory)
                    .findFirst()
                    .map(Path::getFileName)
                    .map(Path::toString);
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
