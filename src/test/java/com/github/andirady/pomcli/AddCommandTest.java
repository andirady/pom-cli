package com.github.andirady.pomcli;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.jimfs.Jimfs;

class AddCommandTest {

    FileSystem fs;

	@BeforeEach
	void setup() {
        fs = Jimfs.newFileSystem();
	}

    @AfterEach
    void cleanup() throws Exception {
        fs.close();
    }

    @Test
    void shouldAdd() throws Exception {
        var pomPath = fs.getPath("pom.xml");

        var cmd = new AddCommand();
        cmd.pomPath = pomPath;
        cmd.coords = new ArrayList<>();
        var d = new Dependency();
        d.setGroupId("g");
        d.setArtifactId("a");
        d.setVersion("1");
        cmd.coords.add(d);
        cmd.run();

        var pat = Pattern.compile(".*<dependency>\\s*<groupId>g</groupId>\\s*<artifactId>a</artifactId>\\s*<version>1</version>\\s*</dependency>.*", Pattern.MULTILINE);
        var s = Files.readString(pomPath);
        var matcher = pat.matcher(s);
        assertNotNull(matcher);
        assertTrue(matcher.find());
    }

    @Test
    void shouldAddScopeIfNotCompileScope() throws Exception {
        var pomPath = fs.getPath("pom.xml");

        var cmd = new AddCommand();
        cmd.pomPath = pomPath;
        cmd.coords = new ArrayList<>();
        var d = new Dependency();
        d.setGroupId("g");
        d.setArtifactId("a");
        d.setVersion("1");
        cmd.coords.add(d);
        cmd.scope = new AddCommand.Scope();
        cmd.scope.test = true;
        cmd.run();

        var pat = Pattern.compile(".*<dependency>\\s*<groupId>g</groupId>\\s*<artifactId>a</artifactId>\\s*<version>1</version>\\s*<scope>test</scope>\\s*</dependency>.*", Pattern.MULTILINE);
        var s = Files.readString(pomPath);
        var matcher = pat.matcher(s);
        assertNotNull(matcher);
        assertTrue(matcher.find());
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

    @Test
    void shouldAddWithoutVersionIfFoundInParentPomDependencyManagement() throws Exception {
        var projectRoot = fs.getPath("app");
        Files.createDirectory(projectRoot);

        var parentPomPath = projectRoot.resolve("pom.xml");
        Files.writeString(parentPomPath, """
            <project>
                <groupId>app</groupId>
                <artifactId>parent</artifactId>
                <version>1</version>
                <packaging>pom</packaging>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>a</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
            """);

        var submodulePath = projectRoot.resolve("child");
        Files.createDirectory(submodulePath);

        var cmd = new AddCommand();
        cmd.pomPath = submodulePath.resolve("pom.xml");
        cmd.coords = new ArrayList<>();
        var d = new Dependency();
        d.setGroupId("a");
        d.setArtifactId("a");
        cmd.coords.add(d);

        cmd.run();

        var p = Pattern.compile("""
                .*<dependencies>\\s*\
                <dependency>\\s*\
                <groupId>a</groupId>\\s*\
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

}
