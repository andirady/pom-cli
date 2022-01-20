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
}
