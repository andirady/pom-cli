/**
 * Copyright 2021-2025 Andi Rady Kurniawan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.andirady.pomcli;

import static java.nio.file.Files.createDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.andirady.pomcli.impl.ConfigTestImpl;
import com.github.andirady.pomcli.impl.GetJavaMajorVersionTestImpl;

import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;

class IdCommandTest extends BaseTest {

    private static final Logger LOG = Logger.getLogger(IdCommandTest.class.getName());

    CommandLine underTest;
    Path projectPath;

    @BeforeEach
    void setup() {
        var app = new Main();
        underTest = new CommandLine(app);
        projectPath = getTempPath();
    }

    @Test
    void shouldFailIfNoIdAndNoExistingPom() {
        var pomPath = projectPath.resolve("pom.xml");

        var ec = underTest.execute("id", "-f", pomPath.toString());
        assertSame(1, ec);
    }

    @Test
    void shouldCreateNewFileIfPomNotExists() throws IOException {
        var pomPath = projectPath.resolve("pom.xml");
        var projectId = "com.example:my-app:0.0.1";

        var ec = underTest.execute("id", "-f", pomPath.toString(), projectId);

        assertSame(0, ec);
        assertTrue(Files.exists(pomPath));
        assertXpath(pomPath, "/project[modelVersion='4.0.0']", 1);
    }

    @Test
    void useCustomDefaultGroupId() throws IOException {

        if (Config.getInstance() instanceof ConfigTestImpl cfg) {
            cfg.setDefaultGroupId("com.example");
        }

        var pomPath = projectPath.resolve(Path.of("my-app", "pom.xml"));
        var expected = "jar com.example:my-app:0.0.1-SNAPSHOT";

        Files.createDirectories(pomPath.getParent());

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));

        var ec = underTest.execute("id", "-f", pomPath.toString(), ".");
        var actual = out.toString().trim();

        assertSame(0, ec);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> javaVersions() {
        return Stream.of(
                Arguments.of(
                        "java 8",
                        """
                                openjdk version "1.8.0_372"
                                OpenJDK Runtime Environment (Temurin)(build 1.8.0_372-b07)
                                OpenJDK 64-Bit Server VM (Temurin)(build 25.372-b07, mixed mode)
                                """,
                        List.of("<maven.compiler.source>1.8</maven.compiler.source>",
                                "<maven.compiler.target>1.8</maven.compiler.target>")

                ),
                Arguments.of(
                        "java 11",
                        """
                                openjdk version "11.0.12" 2021-07-20
                                OpenJDK Runtime Environment 18.9 (build 11.0.12+7)
                                OpenJDK 64-Bit Server VM 18.9 (build 11.0.12+7, mixed mode)
                                """,
                        List.of("<maven.compiler.release>11</maven.compiler.release>")),
                Arguments.of(
                        "java 21-ea",
                        """
                                openjdk version "21-ea" 2023-09-19
                                OpenJDK Runtime Environment (build 21-ea+25-2212)
                                OpenJDK 64-Bit Server VM (build 21-ea+25-2212, mixed mode, sharing)
                                """,
                        List.of("<maven.compiler.release>21</maven.compiler.release>")));
    }

    @ParameterizedTest
    @MethodSource("javaVersions")
    void shouldSetJavaVersionIfCreatingNewPom(String name, String javaVersionOut, List<String> expectedContains)
            throws IOException {
        var pomPath = projectPath.resolve("pom.xml");
        var projectId = "com.example:my-app:0.0.1";

        if (GetJavaMajorVersion.getInstance() instanceof GetJavaMajorVersionTestImpl gjmv) {
            gjmv.setResult(javaVersionOut);
        }

        underTest.execute("id", "-f", pomPath.toString(), projectId);

        var s = Files.readString(pomPath);
        assertTrue(expectedContains.stream().allMatch(s::contains), name);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            foo||jar unnamed:foo:0.0.1-SNAPSHOT
            .||jar unnamed:my-app:0.0.1-SNAPSHOT
            com.example:my-app||jar com.example:my-app:0.0.1-SNAPSHOT
            com.example:my-app|war|war com.example:my-app:0.0.1-SNAPSHOT
            com.example:my-app:1.0.0||jar com.example:my-app:1.0.0
            com.example:my-app:1.0.0|war|war com.example:my-app:1.0.0
            """)
    void shouldMatchExpected(String id, String packaging, String expectedOutput) throws Exception {
        var projectPath = createDirectory(getTempPath().resolve("my-app"));
        var pomPath = projectPath.resolve("pom.xml");

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));

        int ec;

        if (packaging == null) {
            ec = underTest.execute("id", "-f", pomPath.toString(), id);
        } else {
            ec = underTest.execute("id", "-f", pomPath.toString(), "--as=" + packaging, id);
        }

        assertSame(0, ec);
        assertEquals(expectedOutput, out.toString().trim());
    }

    @Test
    void shouldUpdateExistingPom() throws Exception {
        var pomPath = projectPath.resolve("pom.xml");

        var underTest = new CommandLine(new Main());

        underTest.execute("id", "-f", pomPath.toString(), ".");
        assertTrue(Files.exists(pomPath));

        var id = "com.example:my-app";

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));
        underTest.execute("id", "-f", pomPath.toString(), "--as=pom", id);

        assertEquals("pom com.example:my-app:0.0.1-SNAPSHOT", out.toString().trim());
    }

    @Test
    void shouldAddParentIfAPomProjectIsFoundInTheParentDirectory() throws Exception {
        var aPath = createDirectory(projectPath.resolve("a"));
        var bPath = createDirectory(aPath.resolve("b"));
        var parentPomPath = aPath.resolve("pom.xml");
        var pomPath = bPath.resolve("pom.xml");

        var underTest = new CommandLine(new Main());
        // Create the parent
        underTest.execute("id", "-f", parentPomPath.toString(), "--as=pom", "a:a:1");
        LOG.fine(Files.readString(parentPomPath));

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));

        // Create the child
        underTest.execute("id", "-f", pomPath.toString(), ".");
        LOG.fine(Files.readString(pomPath));

        assertXpath(pomPath, "/project/parent[groupId='a' and artifactId='a' and version='1']", 1);
        assertEquals("jar a:b:1", out.toString().trim());
    }

    @Test
    void shouldAddParentIfAPomProjectIsFoundAboveTheDirectoryTree() throws Exception {
        var aPath = createDirectory(projectPath.resolve("a"));
        var bPath = createDirectory(aPath.resolve("b"));
        var cPath = createDirectory(bPath.resolve("c"));
        var parentPomPath = aPath.resolve("pom.xml");
        var pomPath = cPath.resolve("pom.xml");

        var underTest = new CommandLine(new Main());
        // Create the parent
        underTest.execute("id", "-f", parentPomPath.toString(), "--as=pom", "a:a:1");
        LOG.fine(Files.readString(parentPomPath));

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));

        // Create the grandchild
        underTest.execute("id", "-f", pomPath.toString(), ".");
        LOG.fine(Files.readString(pomPath));

        var expr = "/project/parent[groupId='a' and artifactId='a' and version='1' and relativePath='.." + FileSystems.getDefault().getSeparator() + "..']";
        assertXpath(pomPath, expr, 1);
        assertEquals("jar a:c:1", out.toString().trim());
    }

    @Test
    void shouldNotAddParentIfStandalone() throws Exception {
        var aPath = createDirectory(projectPath.resolve("a"));
        var bPath = createDirectory(aPath.resolve("b"));
        var parentPomPath = aPath.resolve("pom.xml");
        var pomPath = bPath.resolve("pom.xml");

        var underTest = new CommandLine(new Main());
        // Create the parent
        underTest.execute("id", "-f", parentPomPath.toString(), "--as=pom", "a:a:1");
        LOG.fine(Files.readString(parentPomPath));

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));

        // Create the child
        underTest.execute("id", "-f", pomPath.toString(), "--standalone", ".");
        LOG.fine(Files.readString(pomPath));

        assertEquals("jar unnamed:b:0.0.1-SNAPSHOT", out.toString().trim());
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', quoteCharacter = '"', value = {
            """
                    ;"<project>
                      <groupId>g</groupId>
                      <artifactId>a</artifactId>
                      <version>${v}</version>
                      <properties>
                        <v>1.0.0</v>
                      </properties>
                    </project>";jar g:a:1.0.0""",
            """
                    "<project>
                      <groupId>g-parent</groupId>
                      <artifactId>a</artifactId>
                      <version>${v}</version>
                      <properties>
                        <v>1.0.0</v>
                      </properties>
                    </project>";"<project>
                      <parent>
                        <groupId>g-parent</groupId>
                        <artifactId>a</artifactId>
                        <version>${v}</version>
                      </parent>
                      <artifactId>a-child</artifactId>
                    </project>";jar g-parent:a-child:1.0.0"""
    })
    void shouldShowPropertyValueWhenVersionIsProperty(
            String parentContent,
            String content,
            String expectedContent)
            throws Exception {
        var pomPath = projectPath.resolve("child").resolve("pom.xml");
        var out = new StringWriter();
        var underTest = new CommandLine(new Main());
        underTest.setOut(new PrintWriter(out));

        if (parentContent != null) {
            Files.writeString(projectPath.resolve("pom.xml"), parentContent);
        }

        Files.createDirectories(pomPath.getParent());
        Files.writeString(pomPath, content);

        var ec = underTest.execute("id", "-f", pomPath.toString());
        var actual = out.toString();
        var expected = Ansi.AUTO.string("%s%n".formatted(expectedContent));

        System.out.println(actual);

        assertSame(0, ec);
        assertEquals(expected, actual);
    }

    @Test
    void shouldAcceptDirectoryAsPomPath() {
        var pomPath = projectPath;
        var out = new StringWriter();
        var underTest = new CommandLine(new Main());
        underTest.setOut(new PrintWriter(out));

        var ec = underTest.execute("id", "-f", pomPath.toString(), "g:a:v");
        var actual = out.toString();

        System.out.println(actual);

        assertSame(0, ec);
    }

    @AfterEach
    void cleanup() throws IOException {
        deleteRecursive(projectPath);
    }

}
