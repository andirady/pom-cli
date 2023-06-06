package com.github.andirady.pomcli;

import org.apache.maven.model.io.DefaultModelReader;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.google.common.jimfs.Jimfs;
import picocli.CommandLine;

class AddCommandTest {

    @TempDir
    Path tempDir;
    FileSystem fs;

    @BeforeEach
    void setup() {
        fs = Jimfs.newFileSystem();
    }

    @AfterEach
    void cleanup() throws Exception {
        fs.close();
    }

    static Stream<Arguments> successArgs() {
        return Stream.of(
            Arguments.of(new String[] { "add", "g:a:1" }, """
                .*<dependency>\\s*\
                <groupId>g</groupId>\\s*\
                <artifactId>a</artifactId>\\s*\
                <version>1</version>\\s*\
                </dependency>.*"""
            ),
            Arguments.of(new String[] { "add", "--compile", "g:a:1" }, """
                .*<dependency>\\s*\
                <groupId>g</groupId>\\s*\
                <artifactId>a</artifactId>\\s*\
                <version>1</version>\\s*\
                </dependency>.*"""
            ),
            Arguments.of(new String[] { "add", "--test", "g:a:1" }, """
                .*<dependency>\\s*\
                <groupId>g</groupId>\\s*\
                <artifactId>a</artifactId>\\s*\
                <version>1</version>\\s*\
                <scope>test</scope>\\s*\
                </dependency>.*"""
            ),
            Arguments.of(new String[] { "add", "--provided", "g:a:1" }, """
                .*<dependency>\\s*\
                <groupId>g</groupId>\\s*\
                <artifactId>a</artifactId>\\s*\
                <version>1</version>\\s*\
                <scope>provided</scope>\\s*\
                </dependency>.*"""
            ),
            Arguments.of(new String[] { "add", "--runtime", "g:a:1" }, """
                .*<dependency>\\s*\
                <groupId>g</groupId>\\s*\
                <artifactId>a</artifactId>\\s*\
                <version>1</version>\\s*\
                <scope>runtime</scope>\\s*\
                </dependency>.*"""
            ),
            Arguments.of(new String[] { "add", "--import", "g:a:1" }, """
                .*<dependencyManagement>\\s*\
                <dependencies>\\s*\
                <dependency>\\s*\
                <groupId>g</groupId>\\s*\
                <artifactId>a</artifactId>\\s*\
                <version>1</version>\\s*\
                <type>pom</type>\\s*\
                <scope>import</scope>\\s*\
                </dependency>\\s*\
                </dependencies>\\s*\
                </dependencyManagement>.*"""
            )
        );
    }

    @ParameterizedTest
    @MethodSource("successArgs")
    void shouldSuccess(String[] args, String expectedPattern) throws Exception {
        var pomPath = fs.getPath("pom.xml");

        try (var paths = Mockito.mockStatic(Path.class)) {
            //paths.when(Path::of).thenReturn(fs.getPath("foo"));
            paths.when(() -> Path.of(ArgumentMatchers.anyString()))
                 .thenAnswer(inv -> fs.getPath((String) inv.getArguments()[0]));

            var app = new Main();
            var cmd = new CommandLine(app);
            cmd.registerConverter(Dependency.class, Main::stringToDependency);
            var rc = cmd.execute(args);
            assertEquals(0, rc);
        }

        var pat = Pattern.compile(expectedPattern, Pattern.MULTILINE);
        var s = Files.readString(pomPath);
        var matcher = pat.matcher(s);
        assertNotNull(matcher);
        assertTrue(matcher.find(), "Does not match /" + expectedPattern + "/ pattern");
    }

    @Test
    void shouldFailIfAlreadAdded() throws Exception {
        var pomPath = fs.getPath("pom.xml");
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

        var cmd = new AddCommand();
        cmd.pomPath = pomPath;
        cmd.coords = new ArrayList<>();
        var d = new Dependency();
        d.setGroupId("g");
        d.setArtifactId("a");
        d.setVersion("2");
        cmd.coords.add(d);
        var e = assertThrows(Exception.class, cmd::run);
        assertEquals("Duplicate artifact(s): g:a", e.getMessage());
    }

    @Test
    void shouldFailIfMultipleAlreadAdded() throws Exception {
        var pomPath = fs.getPath("pom.xml");
        Files.writeString(pomPath, """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>a</groupId>
                        <artifactId>a</artifactId>
                        <version>1</version>
                    </dependency>
                    <dependency>
                        <groupId>b</groupId>
                        <artifactId>b</artifactId>
                        <version>1</version>
                    </dependency>
                </dependencies>
            </project>
            """);

        var cmd = new AddCommand();
        cmd.pomPath = pomPath;
        cmd.coords = new ArrayList<>();
        var d = new Dependency();
        d.setGroupId("a");
        d.setArtifactId("a");
        d.setVersion("2");
        cmd.coords.add(d);
        d = new Dependency();
        d.setGroupId("b");
        d.setArtifactId("b");
        d.setVersion("2");
        cmd.coords.add(d);
        d = new Dependency();
        d.setGroupId("c");
        d.setArtifactId("c");
        d.setVersion("2");
        cmd.coords.add(d);

        var e = assertThrows(Exception.class, cmd::run);
        assertEquals("Duplicate artifact(s): a:a, b:b", e.getMessage());
    }

    private static Stream<Arguments> provideDeps() {
        return Stream.of(
            Arguments.of("g:a"),
            Arguments.of("a")
        );
    }

    @ParameterizedTest
    @MethodSource("provideDeps")
    void shouldReadDependencyManagementFromParentPom(String d) throws Exception {
        var base = fs.getPath("app");
        Files.createDirectory(base);

        var parentPomPath = base.resolve("pom.xml");
        Files.writeString(parentPomPath, """
            <project>
                <groupId>app</groupId>
                <artifactId>parent</artifactId>
                <version>1</version>
                <packaging>pom</packaging>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>g</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
            """);

        var submodulePath = base.resolve("child");
        Files.createDirectory(submodulePath);

        var cmd = new AddCommand();
        cmd.pomPath = submodulePath.resolve("pom.xml");
        cmd.coords = new ArrayList<>();
        cmd.coords.add(Main.stringToDependency(d));

        cmd.run();

        var p = Pattern.compile("""
                .*<dependencies>\\s*\
                <dependency>\\s*\
                <groupId>g</groupId>\\s*\
                <artifactId>a</artifactId>\\s*\
                </dependency>\\s*\
                </dependencies>""", Pattern.MULTILINE);
        var s = Files.readString(cmd.pomPath);
        var m = p.matcher(s);
        assertNotNull(m);
        assertTrue(m.find());
    }

    @Test
    void shouldAddToDependencyManagementIfPackagedAsPom() throws Exception {
        var pomPath = fs.getPath("pom.xml");
        Files.writeString(pomPath, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>a</groupId>
                    <artifactId>a</artifactId>
                    <version>1</version>
                    <packaging>pom</packaging>
                </project>
                """);

        var cmd = new AddCommand();
        cmd.pomPath = pomPath;
        cmd.coords = new ArrayList<>();
        var d = new Dependency();
        d.setGroupId("b");
        d.setArtifactId("b");
        d.setVersion("1");
        cmd.coords.add(d);

        cmd.run();

        var p = Pattern.compile("""
                .*<dependencyManagement>\\s*\
                <dependencies>\\s*\
                <dependency>\\s*\
                <groupId>b</groupId>\\s*\
                <artifactId>b</artifactId>\\s*\
                <version>1</version>\\s*\
                </dependency>\\s*\
                </dependencies>\\s*\
                </dependencyManagement>""", Pattern.MULTILINE);
        var s = Files.readString(pomPath);
        var m = p.matcher(s);
        assertNotNull(m);
        assertTrue(m.find());
    }

    @Test
    void shouldFailIfFileHasNoMavenInfo() throws Exception {
        var libDir = tempDir.resolve("lib");
        var projectDir = tempDir.resolve("project");
        Files.createDirectory(libDir);
        Files.createDirectory(projectDir);

        var jarPath = libDir.resolve("a.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(jarPath))) {
        }

        var app = new Main();
        var cmd = Main.createCommandLine(app);
        var rc = cmd.execute("add", "-f", projectDir.resolve("pom.xml").toString(), jarPath.toString());
        assertSame(picocli.CommandLine.ExitCode.USAGE, rc);

        deleteRecursive(tempDir);
    }

    @Test
    void canAddByReadingMavenInfoFromJarFile() throws Exception {
        var tempDir = Files.createTempDirectory(getClass().getCanonicalName());
        var libDir = tempDir.resolve("lib");
        var projectDir = tempDir.resolve("project");
        Files.createDirectory(libDir);
        Files.createDirectory(projectDir);

        var jarPath = libDir.resolve("a.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(jarPath))) {
            var e = new ZipEntry("META-INF/maven/g/a/pom.properties");
            zip.putNextEntry(e);
            var data = """
                    version=1
                    groupId=g
                    artifactId=a
                    """.getBytes();
            zip.write(data, 0, data.length);
            zip.closeEntry();
        }

        var app = new Main();
        var cmd = Main.createCommandLine(app);
        var rc = cmd.execute("add", "-f", projectDir.resolve("pom.xml").toString(), jarPath.toString());
        assertSame(picocli.CommandLine.ExitCode.OK, rc);

        deleteRecursive(tempDir);
    }

    @Test
    void shouldFailWhenAddingPathWithoutPomXml() throws Exception {
        var tempDir = Files.createTempDirectory(getClass().getCanonicalName());
        var parentDir = tempDir.resolve("hello");
        var apiDir = parentDir.resolve("hello-api");
        var coreDir = parentDir.resolve("hello-core");

        Files.createDirectory(parentDir);
        Files.createDirectory(apiDir);
        Files.createDirectory(coreDir);

        Files.writeString(parentDir.resolve("pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>hello</groupId>
                    <artifactId>hello</artifactId>
                    <version>1</version>
                </project>
                """);
        Files.writeString(coreDir.resolve("pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>hello</groupId>
                        <artifactId>hello</artifactId>
                        <version>1</version>
                    </parent>
                    <artifactId>hello-core</artifactId>
                </project>
                """);

        var app = new Main();
        var cmd = Main.createCommandLine(app);
        var rc = cmd.execute("add", "-f", coreDir.resolve("pom.xml").toString(), apiDir.toString());
        assertSame(picocli.CommandLine.ExitCode.USAGE, rc);

        deleteRecursive(tempDir);
    }

    @Test
    void canAddByPath() throws Exception {
        var tempDir = Files.createTempDirectory(getClass().getCanonicalName());
        var parentDir = tempDir.resolve("hello");
        var apiDir = parentDir.resolve("hello-api");
        var coreDir = parentDir.resolve("hello-core");

        Files.createDirectory(parentDir);
        Files.createDirectory(apiDir);
        Files.createDirectory(coreDir);

        Files.writeString(parentDir.resolve("pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>hello</groupId>
                    <artifactId>hello</artifactId>
                    <version>1</version>
                </project>
                """);
        Files.writeString(apiDir.resolve("pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>hello</groupId>
                        <artifactId>hello</artifactId>
                        <version>1</version>
                    </parent>
                    <artifactId>hello-api</artifactId>
                </project>
                """);
        Files.writeString(coreDir.resolve("pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>hello</groupId>
                        <artifactId>hello</artifactId>
                        <version>1</version>
                    </parent>
                    <artifactId>hello-core</artifactId>
                </project>
                """);

        var app = new Main();
        var cmd = Main.createCommandLine(app);
        var rc = cmd.execute("add", "-f", coreDir.resolve("pom.xml").toString(), apiDir.toString());
        assertSame(picocli.CommandLine.ExitCode.OK, rc);

        var pomReader = new DefaultModelReader();
        try (var is = Files.newInputStream(coreDir.resolve("pom.xml"))) {
            var pom = pomReader.read(is, null);
            assertTrue(pom.getDependencies().stream().anyMatch(d -> "hello-api".equals(d.getArtifactId())));
        }

        deleteRecursive(tempDir);
    }

    private void deleteRecursive(Path dir) throws Exception {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
        }
    }
}
