package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.jimfs.Jimfs;

import picocli.CommandLine.Model.CommandSpec;

class IdCommandTest {


    IdCommand cmd;
    CommandSpec spec;
    FileSystem fs;

    @BeforeEach
    void setup() throws IOException {
        fs = Jimfs.newFileSystem();
        cmd = new IdCommand();
        spec = CommandSpec.forAnnotatedObject(cmd);
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

    private IdCommand newUnnamed() {
        var pomPath = fs.getPath("pom.xml");
        var projectId = "my-app";

        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.run();

        return cmd;
    }

    @Test
    void shouldAddParentIfAPomProjectIsFoundAboveTheDirectoryTree() throws Exception {
    }

    @AfterEach
    void cleanup() throws IOException {
        fs.close();
    }

}
