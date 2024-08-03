package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import picocli.CommandLine;

class PlugCommandTest extends BaseTest {

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

    @ParameterizedTest
    @CsvSource(textBlock = """
            g:a:1,                                      groupId='g' and artifactId='a' and version=1
            maven-resources-plugin,                     artifactId='maven-resources-plugin'
            org.graalvm.buildtools:native-maven-plugin, groupId='org.graalvm.buildtools' and artifactId='native-maven-plugin' and version
            """)
    void basicUse(String input, String subexpr) throws Exception {
        var pomPath = tempDir.resolve("pom.xml");

        var ec = underTest.execute("plug", "-f", pomPath.toString(), input);
        assertSame(0, ec, "Exit code");

        var expr = "/project/build/plugins/plugin[" + subexpr + "]";
        assertXpath(pomPath, expr, 1);
    }
}
