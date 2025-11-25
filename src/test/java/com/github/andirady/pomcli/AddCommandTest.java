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
import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
                       | /project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and not(scope)]
            --compile  | /project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and not(scope)]
            --test     | /project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and scope='test']
            --provided | /project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and scope='provided']
            --runtime  | /project/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and scope='runtime']
            --import   | /project/dependencyManagement/dependencies/dependency[groupId='g' and artifactId='a' and version=1 and scope='import']
            """)
    void addToScope(String scope, String xpathExpr) throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        var gav = "g:a:1";

        var ec = scope == null
                ? underTest.execute("add", "-f", pomPath.toString(), gav)
                : underTest.execute("add", scope, "-f", pomPath.toString(), gav);

        assertSame(0, ec);
        assertXpath(pomPath, xpathExpr, 1, "Nodes matching xpath for scope " + scope);
    }

    @Test
    void failIfDuplicateSingle() throws Exception {
        var pomPath = writeString(tempDir.resolve("pom.xml"), """
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
        var pomPath = writeString(tempDir.resolve("pom.xml"), """
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

    @ParameterizedTest
    @ValueSource(strings = { "g:a", "a" })
    void excludeVersionForParentManaged(String d) throws Exception {
        var parent = writeString(createDirectory(tempDir.resolve("app")).resolve("pom.xml"), """
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
                """).getParent();
        var child = createDirectory(parent.resolve("child"));
        var pomPath = child.resolve("pom.xml");

        var ec = underTest.execute("add", "-f", pomPath.toString(), d);
        assertSame(0, ec);
        assertXpath(pomPath, "/project/dependencies/dependency[groupId='g' and artifactId='a' and not(version)]", 1);
    }

    @Test
    void addAsManangedDependencyForPomPackaging() throws Exception {
        var pomPath = writeString(tempDir.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>a</groupId>
                  <artifactId>a</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                </project>
                """);

        underTest.execute("add", "-f", pomPath.toString(), "b:b:1");

        assertXpath(pomPath, "/project/dependencyManagement/dependencies/dependency[groupId='b']", 1);
    }

    @Test
    void failIfJarHasNoMavenInfo() throws Exception {
        var libDir = createDirectory(tempDir.resolve("lib"));
        var projectDir = createDirectory(tempDir.resolve("project"));

        var jarPath = libDir.resolve("a.jar");
        // Create a blank jar
        try (var _ = new ZipOutputStream(Files.newOutputStream(jarPath))) {
        }

        var ec = underTest.execute("add", "-f", projectDir.resolve("pom.xml").toString(), jarPath.toString());
        assertSame(picocli.CommandLine.ExitCode.USAGE, ec);
    }

    @Test
    void addByJarWithMavenInfo() throws Exception {
        var libDir = createDirectory(tempDir.resolve("lib"));
        var projectDir = createDirectory(tempDir.resolve("project"));

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
        var parentDir = createDirectory(tempDir.resolve("hello"));
        var apiDir = createDirectory(parentDir.resolve("hello-api"));
        var coreDir = createDirectory(parentDir.resolve("hello-core"));

        writeString(parentDir.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>hello</groupId>
                  <artifactId>hello</artifactId>
                  <version>1</version>
                </project>
                """);
        var pomPath = writeString(coreDir.resolve("pom.xml"), """
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
        assertSame(picocli.CommandLine.ExitCode.USAGE, ec);
    }

    @Test
    void canAddByPath() throws Exception {
        var parentDir = createDirectory(tempDir.resolve("hello"));
        var apiDir = createDirectory(parentDir.resolve("hello-api"));
        var coreDir = createDirectory(parentDir.resolve("hello-core"));

        writeString(parentDir.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>hello</groupId>
                  <artifactId>hello</artifactId>
                  <version>1</version>
                </project>
                """);
        writeString(apiDir.resolve("pom.xml"), """
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

        var pomPath = writeString(coreDir.resolve("pom.xml"), """
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
        assertXpath(pomPath, "/project/dependencies/dependency[artifactId='hello-api']", 1);
    }

    @Test
    void versionAutoResolution() {
        var pomPath = tempDir.resolve("pom.xml");
        var ec = underTest.execute("add", "-f", pomPath.toString(), "com.fasterxml.jackson.core:jackson-databind");
        assertSame(0, ec);

        assertXpath(pomPath,
                "/project/dependencies/dependency[groupId='com.fasterxml.jackson.core' and artifactId='jackson-databind' and version]",
                1);
    }

    @ParameterizedTest
    @MethodSource
    void addManagedFailOnDuplicate(PathFunction pomPathCreator) throws IOException {
        var pomPath = pomPathCreator.apply(tempDir);
        var ec = underTest.execute("add", "-d", "-f", pomPath.toString(), "jackson-databind");
        assertSame(1, ec);
    }

    static Stream<PathFunction> addManagedFailOnDuplicate() {
        return Stream.of(
                // Dependency managed by parent
                (tempDir) -> writeString(createDirectory(writeString(tempDir.resolve("pom.xml"), """
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
                        """).getParent().resolve("api")).resolve("pom.xml"), """
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
                        """),
                // Dependency managed by remote parent
                (tempDir) -> writeString(tempDir.resolve("pom.xml"), """
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
                        """),
                // Dependency managed by imported dependency
                (tempDir) -> writeString(tempDir.resolve("pom.xml"), """
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
                        """));
    }

    @ParameterizedTest
    @MethodSource
    void addManaged(PathFunction pomPathCreator) throws IOException {
        var pomPath = pomPathCreator.apply(tempDir);
        var ec = underTest.execute("add", "-f", pomPath.toString(), "jackson-databind");
        assertSame(0, ec);
        assertXpath(pomPath,
                "/project/dependencies/dependency[groupId='com.fasterxml.jackson.core' and artifactId='jackson-databind' and not(version)]",
                1);
    }

    static Stream<PathFunction> addManaged() {
        return Stream.of(
                // Dependency managed by parent
                (tempDir) -> writeString(createDirectory(writeString(tempDir.resolve("pom.xml"), """
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
                        """).getParent().resolve("api")).resolve("pom.xml"), """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>g</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                            <relativePath>..</relativePath>
                          </parent>
                        </project>
                        """),
                // Dependency managed by remote parent
                (tempDir) -> writeString(tempDir.resolve("pom.xml"), """
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
                        """),
                // Dependency managed by imported dependency
                (tempDir) -> writeString(tempDir.resolve("pom.xml"), """
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
                        """));
    }

    @Test
    void shouldSuccesfullyAddOptional() throws IOException {
        var pomPath = tempDir.resolve("pom.xml");
        underTest.execute("add", "-f", pomPath.toString(), "--optional", "g:a:1.0.0");
        assertXpath(pomPath,
                "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1.0.0' and optional='true']",
                1);
    }

    @Test
    void shouldFailAddOptionalWhenPackagingIsPom() throws IOException {
        var pomPath = tempDir.resolve("pom.xml");
        writeString(pomPath, """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>hello-app</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """);
        var ec = underTest.execute("add", "-f", pomPath.toString(), "--optional", "g:a:1.0.0");
        assertNotSame(0, ec);
    }

    @Test
    void shouldNotAddExclusionWhenNotSpecified() {
        var pomPath = tempDir.resolve("pom.xml");
        underTest.execute("add", "-f", pomPath.toString(), "g:a:1.0.0");

        assertXpath(pomPath,
                "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1.0.0']/exclusions/exclusion",
                0);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            b,groupId='*' and artifactId='b'
            hello:world,groupId='hello' and artifactId='world'
            """)
    void shouldAddSingleExclusion(String excludes, String predicate) throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        writeString(pomPath, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>hello-app</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        underTest.execute("add", "-f", pomPath.toString(), "g:a:1.0.0", "--excludes", excludes);
        assertXpath(pomPath,
                "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1.0.0']/exclusions/exclusion[%s]"
                        .formatted(predicate),
                1, predicate);
    }

    @Test
    void shouldAddMultipleExclusions() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        writeString(pomPath, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>hello-app</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        underTest.execute("add", "-f", pomPath.toString(), "g:a:1.0.0", "--excludes", "g:b,g:c");
        assertXpath(pomPath,
                "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1.0.0']/exclusions/exclusion",
                2);
    }

    @Test
    void shouldAcceptDirectoryAsPomPath() throws Exception {
        var projectDir = createDirectory(tempDir.resolve("project"));
        var pomPath = projectDir.resolve("pom.xml");
        writeString(pomPath, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>hello-app</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        var ec = underTest.execute("add", "-f", projectDir.toString(), "g:a:1.0.0");
        assertSame(0, ec);
        assertXpath(pomPath,
                "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1.0.0']",
                1);
    }

    @Test
    void shouldAcceptDirectoryAsPomPathAndCreatePomIfNotExists() throws Exception {
        var projectDir = createDirectory(tempDir.resolve("project"));
        var pomPath = projectDir.resolve("pom.xml");
        var ec = underTest.execute("add", "-f", projectDir.toString(), "g:a:1.0.0");
        assertSame(0, ec);
        assertXpath(pomPath,
                "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1.0.0']",
                1);
    }

    @Test
    void shouldSuccessWhenImportDependencyVersionIsProperty() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        writeString(pomPath, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>a</groupId>
                  <artifactId>a</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.apache.logging.log4j</groupId>
                        <artifactId>log4j</artifactId>
                        <version>${version.prop}</version>
                        <scope>import</scope>
                        <type>pom</type>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <properties>
                    <version.prop>2.25.2</version.prop>
                  </properties>
                </project>
                """);

        var ec = underTest.execute("add", "-f", pomPath.toString(), "-d", "log4j-api");
        assertSame(0, ec);
        assertXpath(pomPath,
                "/project/dependencies/dependency[groupId='org.apache.logging.log4j' and artifactId='log4j-api' and not(version)]",
                1);
    }

    @FunctionalInterface
    interface PathFunction {

        Path apply(Path path) throws IOException;
    }

}
