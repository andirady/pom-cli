package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import picocli.CommandLine;

class AddCommandTest extends BaseTest {

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

    static Stream<Arguments> addToScope() {
        return Stream.of(
                Arguments.of(null,
                        "/project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and not(scope)]"),
                Arguments.of("--compile",
                        "/project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and not(scope)]"),
                Arguments.of("--test",
                        "/project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and scope='test']"),
                Arguments.of("--provided",
                        "/project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and scope='provided']"),
                Arguments.of("--runtime",
                        "/project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and scope='runtime']"),
                Arguments.of("--import",
                        "/project/dependencyManagement/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and scope='import']"));
    }

    @ParameterizedTest
    @MethodSource
    void addToScope(String scope, String xpathExpr) throws Exception {
        var pomPath = tempDir.resolve("pom.xml");

        var gav = "g:a:1";
        var ec = scope == null
                ? underTest.execute("add", "-f", pomPath.toString(), gav)
                : underTest.execute("add", scope, "-f", pomPath.toString(), gav);

        assertSame(0, ec);
        assertSame(1, evalXpath(pomPath, xpathExpr), "Nodes matching xpath for scope " + scope);
    }

    @Test
    void failIfDuplicateSingle() throws Exception {
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

        var ec = underTest.execute("add", "-f", pomPath.toString(), "g:a:2");
        assertSame(1, ec);
    }

    @Test
    void failIfDuplicateMultiple() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
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

        var ec = underTest.execute("add", "-f", pomPath.toString(), "a:a:2", "b:b:2");
        assertSame(1, ec);
    }

    static Stream<Arguments> excludeVersionForParentManaged() {
        return Stream.of(
                Arguments.of("g:a"),
                Arguments.of("a"));
    }

    @ParameterizedTest
    @MethodSource
    void excludeVersionForParentManaged(String d) throws Exception {
        var base = tempDir.resolve("app");
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
        var pomPath = submodulePath.resolve("pom.xml");
        Files.createDirectory(submodulePath);

        var ec = underTest.execute("add", "-f", pomPath.toString(), d);
        assertSame(0, ec);

        var matched = evalXpath(pomPath,
                "/project/dependencies/dependency[groupId='g' and artifactId='a' and not(version)]");
        assertSame(1, matched, "Nodes matching");
    }

    @Test
    void addAsManangedDependencyForPomPackaging() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        Files.writeString(pomPath, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>a</groupId>
                    <artifactId>a</artifactId>
                    <version>1</version>
                    <packaging>pom</packaging>
                </project>
                """);

        underTest.execute("add", "-f", pomPath.toString(), "b:b:1");

        var matched = evalXpath(pomPath, "/project/dependencyManagement/dependencies/dependency[groupId='b']");
        assertSame(1, matched, "Nodes matching");
    }

    @Test
    void failIfJarHasNoMavenInfo() throws Exception {
        var libDir = tempDir.resolve("lib");
        var projectDir = tempDir.resolve("project");
        Files.createDirectory(libDir);
        Files.createDirectory(projectDir);

        var jarPath = libDir.resolve("a.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(jarPath))) {
        }

        var ec = underTest.execute("add", "-f", projectDir.resolve("pom.xml").toString(), jarPath.toString());
        assertSame(picocli.CommandLine.ExitCode.USAGE, ec);
    }

    @Test
    void addByJarWithMavenInfo() throws Exception {
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

        var ec = underTest.execute("add", "-f", projectDir.resolve("pom.xml").toString(), jarPath.toString());
        assertSame(0, ec);
    }

    @Test
    void shouldFailWhenAddingPathWithoutPomXml() throws Exception {
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

        var ec = underTest.execute("add", "-f", coreDir.resolve("pom.xml").toString(), apiDir.toString());
        assertSame(picocli.CommandLine.ExitCode.USAGE, ec);
    }

    @Test
    void canAddByPath() throws Exception {
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

        var pomPath = coreDir.resolve("pom.xml");
        Files.writeString(pomPath, """
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

        var ec = underTest.execute("add", "-f", pomPath.toString(), apiDir.toString());
        assertSame(0, ec);

        var matched = evalXpath(pomPath, "/project/dependencies/dependency[artifactId='hello-api']");
        assertSame(1, matched, "Nodes matching");
    }

    @Test
    void versionAutoResolution() {
        var pomPath = tempDir.resolve("pom.xml");
        var ec = underTest.execute("add", "-f", pomPath.toString(), "com.fasterxml.jackson.core:jackson-databind");
        assertSame(0, ec);

        var matched = evalXpath(pomPath,
                "/project/dependencies/dependency[groupId='com.fasterxml.jackson.core' and artifactId='jackson-databind' and version]");
        assertSame(1, matched, "Nodes matching");
    }

    @ParameterizedTest
    @MethodSource
    void addManagedFailOnDuplicate(Function<Path, Path> pomPathCreator) throws IOException {
        var pomPath = pomPathCreator.apply(tempDir);
        var ec = underTest.execute("add", "-d", "-f", pomPath.toString(), "jackson-databind");
        assertSame(1, ec);
    }

    static Stream<Function<Path, Path>> addManagedFailOnDuplicate() {
        return Stream.of((tempDir) -> {
            var parentPomPath = tempDir.resolve("pom.xml");
            var childDir = tempDir.resolve("api");

            try {
                Files.writeString(parentPomPath, """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <groupId>g</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                            <packaging>pom</packaging>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.fasterxml.jackson.core</groupId>
                                        <artifactId>jackson-databind</artifactId>
                                        <version>2.14.0</version>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                            <modules>
                                <module>api</module>
                            </modules>
                        </project>
                        """);

                Files.createDirectory(childDir);
                var pomPath = childDir.resolve("pom.xml");

                Files.writeString(pomPath, """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <parent>
                                <groupId>g</groupId>
                                <artifactId>a</artifactId>
                                <version>1</version>
                                <relativePath>..</relativePath>
                            </parent>
                            <dependencies>
                                <dependency>
                                    <groupId>com.fasterxml.jackson.core</groupId>
                                    <artifactId>jackson-databind</artifactId>
                                </dependency>
                            </dependencies>
                        </project>
                        """);
                return pomPath;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, (tempDir) -> {
            var pomPath = tempDir.resolve("pom.xml");
            try {
                Files.writeString(pomPath, """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <parent>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter-parent</artifactId>
                                <version>3.1.0</version>
                            </parent>
                            <artifactId>a</artifactId>
                            <version>1</version>
                            <dependencies>
                                <dependency>
                                    <groupId>com.fasterxml.jackson.core</groupId>
                                    <artifactId>jackson-databind</artifactId>
                                </dependency>
                            </dependencies>
                        </project>
                        """);
                return pomPath;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, (tempDir) -> {
            var pomPath = tempDir.resolve("pom.xml");
            try {
                Files.writeString(pomPath, """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <groupId>g</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>org.springframework.boot</groupId>
                                        <artifactId>spring-boot-dependencies</artifactId>
                                        <version>3.1.0</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>com.fasterxml.jackson.core</groupId>
                                    <artifactId>jackson-databind</artifactId>
                                </dependency>
                            </dependencies>
                        </project>
                        """);
                return pomPath;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @ParameterizedTest
    @MethodSource
    void addManaged(Function<Path, Path> pomPathCreator) throws IOException {
        var pomPath = pomPathCreator.apply(tempDir);
        var ec = underTest.execute("add", "-f", pomPath.toString(), "jackson-databind");
        assertSame(0, ec);

        var matched = evalXpath(pomPath,
                "/project/dependencies/dependency[groupId='com.fasterxml.jackson.core' and artifactId='jackson-databind' and not(version)]");
        assertSame(1, matched, "Nodes matching");
    }

    static Stream<Function<Path, Path>> addManaged() {
        return Stream.of((tempDir) -> {
            var parentPomPath = tempDir.resolve("pom.xml");
            var childDir = tempDir.resolve("api");

            try {
                Files.writeString(parentPomPath, """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <groupId>g</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                            <packaging>pom</packaging>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.fasterxml.jackson.core</groupId>
                                        <artifactId>jackson-databind</artifactId>
                                        <version>2.14.0</version>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                            <modules>
                                <module>api</module>
                            </modules>
                        </project>
                        """);

                Files.createDirectory(childDir);
                var pomPath = childDir.resolve("pom.xml");

                Files.writeString(pomPath, """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <parent>
                                <groupId>g</groupId>
                                <artifactId>a</artifactId>
                                <version>1</version>
                                <relativePath>..</relativePath>
                            </parent>
                        </project>
                        """);
                return pomPath;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, (tempDir) -> {
            var pomPath = tempDir.resolve("pom.xml");
            try {
                Files.writeString(pomPath, """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <parent>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter-parent</artifactId>
                                <version>3.1.0</version>
                            </parent>
                            <artifactId>a</artifactId>
                            <version>1</version>
                        </project>
                        """);
                return pomPath;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, (tempDir) -> {
            var pomPath = tempDir.resolve("pom.xml");
            try {
                Files.writeString(pomPath, """
                        <project>
                            <modelVersion>4.0.0</modelVersion>
                            <groupId>g</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>org.springframework.boot</groupId>
                                        <artifactId>spring-boot-dependencies</artifactId>
                                        <version>3.1.0</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                        </project>
                        """);
                return pomPath;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @FunctionalInterface
    public interface SilentSupplier<T> {
        T get() throws Exception;

        default T getSilently() {
            try {
                return get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
