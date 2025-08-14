package com.github.andirady.pomcli;

import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

class UnsetCommandTest extends BaseTest {

    CommandLine underTest;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        var app = new Main();
        underTest = Main.createCommandLine(app);
    }

    @AfterEach
    void cleanUp() {
        deleteRecursive(tempDir);
    }

    @Test
    void shouldFailWhenPropertyDoesNotExists() throws IOException {
        var pomPath = tempDir.resolve("pom.xml");
        writeString(pomPath, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>g</groupId>
                    <artifactId>a</artifactId>
                    <version>1</version>
                    <properties>
                    </properties>
                </project>
                """);

        var result = underTest.execute("unset", "-f", pomPath.toString(), "target");
        assertTrue(result > 0);
    }

    @Test
    void shouldFailWhenPropertyDoesNotExistsInProfile() throws IOException {
        var pomPath = tempDir.resolve("pom.xml");
        writeString(pomPath, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>g</groupId>
                    <artifactId>a</artifactId>
                    <version>1</version>
                    <properties>
                    </properties>
                    <profiles>
                        <profile>
                            <id>target</id>
                            <properties>
                            </properties>
                        </profile>
                    </profiles>
                </project>
                """);

        var result = underTest.execute("-P", "target", "unset", "-f", pomPath.toString(), "target");
        assertTrue(result > 0);
    }

    @Test
    void shouldRemoveFromProperties() throws IOException {
        var pomPath = tempDir.resolve("pom.xml");
        writeString(pomPath, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>g</groupId>
                    <artifactId>a</artifactId>
                    <version>1</version>
                    <properties>
                        <target>foo</target>
                    </properties>
                </project>
                """);

        underTest.execute("unset", "-f", pomPath.toString(), "target");
        assertXpath(pomPath, "/project/properties/target", 0);
    }

    @Test
    void shouldRemoveFromProfileProperties() throws IOException {
        var pomPath = tempDir.resolve("pom.xml");
        writeString(pomPath, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>g</groupId>
                    <artifactId>a</artifactId>
                    <version>1</version>
                    <properties>
                        <target>foo</target>
                    </properties>
                    <profiles>
                        <profile>
                            <id>target</id>
                            <properties>
                                <target>bar</target>
                            </properties>
                        </profile>
                        <profile>
                            <id>nontarget</id>
                            <properties>
                                <target>baz</target>
                            </properties>
                        </profile>
                    </profiles>
                </project>
                """);

        underTest.execute("-P", "target", "unset", "-f", pomPath.toString(), "target");

        System.out.println(Files.readString(pomPath));
        assertXpath(pomPath, "/project/properties/target", 1);
        assertXpath(pomPath, "/project/profiles/profile[id='target']/properties/target", 0);
        assertXpath(pomPath, "/project/profiles/profile[id='nontarget']/properties/target", 1);
    }
}
