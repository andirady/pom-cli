package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class BaseTest {

    protected static final Path TEST_REPO_PATH = Path.of("target", "test-classes", "m2", "repository");
    protected static final Path TEST_PROJECTS_PATH = Path.of("target", "test-classes", "projects");

    @BeforeAll
    static void ensureDirs() throws Exception {
        Files.createDirectories(TEST_REPO_PATH);
        Files.createDirectories(TEST_PROJECTS_PATH);

        assertTrue(Files.isDirectory(TEST_REPO_PATH));
        assertTrue(Files.isDirectory(TEST_PROJECTS_PATH));
    }

    Path getPath(String path) {
        return getPath(Path.of(path));
    }

    Path getPath(Path path) {
        return TEST_PROJECTS_PATH.resolve(path);
    }

    Path getTempPath() {
        try {
            return Files.createTempDirectory(TEST_PROJECTS_PATH, "tmp");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void deleteRecursive(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.list(path).forEach(BaseTest::deleteRecursive);
            }

            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterAll
    static void deleteTempDirs() throws IOException {
        Files.list(TEST_PROJECTS_PATH)
             .filter(p -> p.getFileName().startsWith("tmp"))
             .forEach(BaseTest::deleteRecursive);
    }

}
