package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;

class RemoveCommandTest extends BaseTest {

    CommandLine underTest;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        var app = new Main();
        underTest = Main.createCommandLine(app);
    }

    @Test
    void removeNonDependency() throws IOException {
        var pomPath = tempDir.resolve("pom.xml");
        Files.writeString(pomPath, """
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>g</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        var ec = underTest.execute("rm", "-f", pomPath.toString(), "b");
        assertSame(0, ec);
        var matched = evalXpath(pomPath,
                "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1']");
        assertSame(1, matched, "Nodes matching");
    }

    @ParameterizedTest
    @ValueSource(strings = { "g:a:1", "g:a", "a" })
    void success(String coord) throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        Files.writeString(pomPath, """
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>g</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        var ec = underTest.execute("rm", "-f", pomPath.toString(), coord);
        assertSame(0, ec);
        var matched = evalXpath(pomPath,
                "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1']");
        assertSame(0, matched, "Nodes matching");
    }
}
