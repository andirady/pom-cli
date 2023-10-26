package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.andirady.pomcli.impl.ConfigTestImpl;
import com.github.andirady.pomcli.impl.GetJavaMajorVersionTestImpl;

import picocli.CommandLine;

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
        var s = Files.readString(pomPath);
        assertTrue(s.contains("<modelVersion>4.0.0</modelVersion>"));
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
                List.of("<maven.compiler.release>11</maven.compiler.release>")
            ),
            Arguments.of(
                "java 21-ea",
                """
                openjdk version "21-ea" 2023-09-19
                OpenJDK Runtime Environment (build 21-ea+25-2212)
                OpenJDK 64-Bit Server VM (build 21-ea+25-2212, mixed mode, sharing)
                """,
                List.of("<maven.compiler.release>21</maven.compiler.release>")
            )
        );
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

    private static Stream<Arguments> idInputs() {
        return Stream.of(
            Arguments.of("foo", null, "jar unnamed:foo:0.0.1-SNAPSHOT"),
            Arguments.of(".", null, "jar unnamed:my-app:0.0.1-SNAPSHOT"),
            Arguments.of("com.example:my-app", null, "jar com.example:my-app:0.0.1-SNAPSHOT"),
            Arguments.of("com.example:my-app", "war", "war com.example:my-app:0.0.1-SNAPSHOT"),
            Arguments.of("com.example:my-app:1.0.0", null, "jar com.example:my-app:1.0.0"),
            Arguments.of("com.example:my-app:1.0.0", "war", "war com.example:my-app:1.0.0")
        );
    }

    @ParameterizedTest
    @MethodSource("idInputs")
    void shouldMatchExpected(String id, String packaging, String expectedOutput) throws Exception {
        var projectPath = getTempPath().resolve("my-app");
        var pomPath = projectPath.resolve("pom.xml");

        Files.createDirectory(projectPath);

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
        var aPath = projectPath.resolve("a");
        var bPath = aPath.resolve("b");
        var parentPomPath = aPath.resolve("pom.xml");
        var pomPath = bPath.resolve("pom.xml");

        Files.createDirectory(aPath);
        Files.createDirectory(bPath);

        var underTest = new CommandLine(new Main());
        // Create the parent
        underTest.execute("id", "-f", parentPomPath.toString(), "--as=pom", "a:a:1");
        LOG.fine(Files.readString(parentPomPath));

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));

        // Create the child
        underTest.execute("id", "-f", pomPath.toString(), ".");
        LOG.fine(Files.readString(pomPath));

        var pat = Pattern.compile("""
                .*<parent>\\s*\
                <groupId>a</groupId>\\s*\
                <artifactId>a</artifactId>\\s*\
                <version>1</version>\\s*\
                </parent>""", Pattern.MULTILINE);
        var s = Files.readString(pomPath);
        var matcher = pat.matcher(s);
        assertNotNull(matcher);
        assertTrue(matcher.find());
        assertEquals("jar a:b:1", out.toString().trim());
    }

    @Test
    void shouldAddParentIfAPomProjectIsFoundAboveTheDirectoryTree() throws Exception {
        var aPath = projectPath.resolve("a");
        var bPath = aPath.resolve("b");
        var cPath = bPath.resolve("c");
        var parentPomPath = aPath.resolve("pom.xml");
        var pomPath = cPath.resolve("pom.xml");

        Files.createDirectory(aPath);
        Files.createDirectory(bPath);
        Files.createDirectory(cPath);

        var underTest = new CommandLine(new Main());
        // Create the parent
        underTest.execute("id", "-f", parentPomPath.toString(), "--as=pom", "a:a:1");
        LOG.fine(Files.readString(parentPomPath));

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));

        // Create the grandchild
        underTest.execute("id", "-f", pomPath.toString(), ".");
        LOG.fine(Files.readString(pomPath));

        var pat = Pattern.compile("""
                .*<parent>\\s*\
                <groupId>a</groupId>\\s*\
                <artifactId>a</artifactId>\\s*\
                <version>1</version>\\s*\
                <relativePath>../..</relativePath>\\s*\
                </parent>""", Pattern.MULTILINE);
        var s = Files.readString(pomPath);
        var matcher = pat.matcher(s);
        assertNotNull(matcher);
        assertTrue(matcher.find());
        assertEquals("jar a:c:1", out.toString().trim());
    }

    @Test
    void shouldNotAddParentIfStandalone() throws Exception {
        var aPath = projectPath.resolve("a");
        var bPath = aPath.resolve("b");
        var parentPomPath = aPath.resolve("pom.xml");
        var pomPath = bPath.resolve("pom.xml");

        Files.createDirectory(aPath);
        Files.createDirectory(bPath);

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

    @AfterEach
    void cleanup() throws IOException {
        deleteRecursive(projectPath);
    }

}
