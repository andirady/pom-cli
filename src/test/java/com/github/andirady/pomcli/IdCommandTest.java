package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdCommandTest {

    private Path tempDir;

    @BeforeEach
    void setup() throws IOException {
        tempDir = Files.createTempDirectory("mq-test");
    }

    @Test
    void shouldCreateNewFileIfPomNotExists() {
        var pomPath = tempDir.resolve("pom.xml");
        var projectId = "com.example:my-app:0.0.1";

        var cmd = new IdCommand();
        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.run();
        assertTrue(Files.exists(cmd.pomPath));
    }

    @Test
    void shouldAutomaticallySetVersionIfNotSpecified() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        var projectId = "com.example:my-app";

        var cmd = new IdCommand();
        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.run();
        assertEquals("jar com.example:my-app:0.0.1-SNAPSHOT", cmd.readProjectId());
    }
    
    @Test
    void shouldSetPackaging() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        var projectId = "com.example:my-app:0.0.1";
        var packaging = "war";

        var cmd = new IdCommand();
        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.as = packaging;
        cmd.run();
        assertEquals("war com.example:my-app:0.0.1", cmd.readProjectId());
    }
    
    @Test
    void shouldSetGroupIdToUnnamedIfNotSpeficied() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        var projectId = "my-app";

        var cmd = new IdCommand();
        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.run();
        assertEquals("jar unnamed:my-app:0.0.1-SNAPSHOT", cmd.readProjectId());    
    }
    
    @Test
    void shouldUpdateExistingPom() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        var projectId = "com.example:my-app";

        shouldSetGroupIdToUnnamedIfNotSpeficied();
        
        var cmd = new IdCommand();
        cmd.pomPath = pomPath;
        cmd.id = projectId;
        cmd.as = "pom";
        cmd.run();
        assertEquals("pom com.example:my-app:0.0.1-SNAPSHOT", cmd.readProjectId());
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.walk(tempDir).map(Path::toFile).forEach(File::delete);
        Files.delete(tempDir);
    }

}
