package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.w3c.dom.NodeList;

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

    private int evalXpath(Path pomPath, String expr) {
        try (var is = Files.newInputStream(pomPath)) {
            var dbf = DocumentBuilderFactory.newInstance();
            var db = dbf.newDocumentBuilder();
            var doc = db.parse(is);
            var xpath = XPathFactory.newInstance().newXPath();

            return ((NodeList) xpath.evaluate(expr, doc, XPathConstants.NODESET)).getLength();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void assertXpath(Path pomPath, String expr, int count) {
        assertXpath(pomPath, expr, count, "Nodes matching");
    }

    void assertXpath(Path pomPath, String expr, int count, String message) {
        assertSame(count, evalXpath(pomPath, expr), message);
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
