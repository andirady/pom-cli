package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystem;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.jimfs.Jimfs;

import picocli.CommandLine;

class IdCommandTest {


    IdCommand cmd;
    FileSystem fs;

    @BeforeEach
    void setup() {
        fs = Jimfs.newFileSystem();
        cmd = new IdCommand();
    }

    @Test
    void shouldCreateNewFileIfPomNotExists() {
        var pomPath = fs.getPath("pom.xml");
        var projectId = "com.example:my-app:0.0.1";

        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.run();
        assertTrue(Files.exists(cmd.pomPath));
    }

    @Test
    void shouldSetModelVersionIfCreatingNewPom() throws IOException {
        var pomPath = fs.getPath("pom.xml");
        var projectId = "com.example:my-app:0.0.1";

        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.run();

        var s = Files.readString(pomPath);
        assertTrue(s.contains("<modelVersion>4.0.0</modelVersion>"));
    }

    @Test
    void shouldSetJavaVersionIfCreatingNewPom() throws IOException {
        var pomPath = fs.getPath("pom.xml");
        var projectId = "com.example:my-app:0.0.1";

        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.run();

        var s = Files.readString(pomPath);
        assertTrue(s.contains("<maven.compiler.source>"));
        assertTrue(s.contains("<maven.compiler.target>"));
    }

    @Test
    void shouldAutomaticallySetVersionIfNotSpecified() throws Exception {
        var pomPath = fs.getPath("pom.xml");
        var projectId = "com.example:my-app";

        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.run();
        assertEquals("jar com.example:my-app:0.0.1-SNAPSHOT", cmd.readProjectId());
    }
    
    @Test
    void shouldSetPackaging() throws Exception {
        var pomPath = fs.getPath("pom.xml");
        var projectId = "com.example:my-app:0.0.1";
        var packaging = "war";

        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.as = packaging;
        cmd.run();
        assertEquals("war com.example:my-app:0.0.1", cmd.readProjectId());
    }
    
    @Test
    void shouldSetGroupIdToUnnamedIfNotSpeficied() throws Exception {
        var cmd = newUnnamed();
        assertEquals("jar unnamed:my-app:0.0.1-SNAPSHOT", cmd.readProjectId());    
    }
    
    @Test
    void shouldUpdateExistingPom() throws Exception {
        newUnnamed();

        var pomPath = fs.getPath("pom.xml");
        var projectId = "com.example:my-app";

        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.as = "pom";
        cmd.run();
        assertEquals("pom com.example:my-app:0.0.1-SNAPSHOT", cmd.readProjectId());
    }

    @Test
    void shouldAddParentIfAPomProjectIsFoundAboveTheDirectoryTree() throws Exception {
        var aPath = fs.getPath("a");
        var bPath = aPath.resolve("b");
        var cPath = bPath.resolve("c");
        var parentPomPath = aPath.resolve("pom.xml");
        var pomPath = cPath.resolve("pom.xml");

        Files.createDirectory(aPath);
        Files.createDirectory(bPath);
        Files.createDirectory(cPath);

        cmd.pomPath = parentPomPath;
        cmd.id = "a:a:1";
        cmd.as = "pom";
        cmd.run();

        cmd.pomPath = pomPath;
        cmd.id = "c";
        cmd.as = "jar";
        cmd.run();

        var pat = Pattern.compile(".*<parent>\\s*<groupId>a</groupId>\\s*<artifactId>a</artifactId>\\s*<version>1</version>\\s*<relativePath>../../..</relativePath>\\s*</parent>", Pattern.MULTILINE);
        var s = Files.readString(pomPath);
        var matcher = pat.matcher(s);
        assertNotNull(matcher);
        assertTrue(matcher.find());
        assertEquals("jar a:c:1", cmd.readProjectId());
    }

    private IdCommand newUnnamed() {
        var pomPath = fs.getPath("pom.xml");
        var projectId = "my-app";

        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.run();

        return cmd;
    }

    @AfterEach
    void cleanup() throws IOException {
        fs.close();
    }

}
