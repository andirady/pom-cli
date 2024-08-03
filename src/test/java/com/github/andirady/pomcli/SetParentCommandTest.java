package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

class SetParentCommandTest extends BaseTest {

    CommandLine underTest;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        var app = new Main();
        underTest = new CommandLine(app);
    }

    @AfterEach
    void cleanup() {
        deleteRecursive(tempDir);
    }

    @Test
    void test() {
        var pomPath = tempDir.resolve("pom.xml");

        var ec = underTest.execute("parent", "-f", pomPath.toString(), "com.example:some-parent:1.0.0");
        assertSame(0, ec, "Exit code");

        var expr = "/project/parent[groupId='com.example' and artifactId='some-parent' and version='1.0.0']";
        assertXpath(pomPath, expr, 1);
    }
}
