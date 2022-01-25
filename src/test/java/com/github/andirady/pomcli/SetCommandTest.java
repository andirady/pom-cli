package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.jimfs.Jimfs;

class SetCommandTest {

    FileSystem fs;

    @BeforeEach
    void setup() throws Exception {
        fs = Jimfs.newFileSystem();
    }

    @Test
    void shouldFailIfPomNotExists() {
        var pomPath = fs.getPath("pom.xml");

        var cmd = new SetCommand();
        cmd.pomPath = pomPath;
        cmd.properties = List.of(new SetCommand.Property("a", "1"));
        assertThrows(IllegalStateException.class, cmd::run);
    }

    @Test
    void shouldAddProperty() throws Exception {
        var pomPath = fs.getPath("pom.xml");
        Files.writeString(pomPath, "<project></project>");

        var cmd = new SetCommand();
        cmd.pomPath = pomPath;
        cmd.properties = List.of(new SetCommand.Property("foo.bar", "1"));
        cmd.run();

        var s = Files.readString(pomPath);
        var pat = Pattern.compile(".*<properties>\\s*<foo.bar>1</foo.bar>\\s*</properties>.*", Pattern.MULTILINE);
        var m = pat.matcher(s);
        assertNotNull(m);
        assertTrue(m.find());
    }

    @Test
    void shouldAddMultipleProperties() throws Exception {
        var pomPath = fs.getPath("pom.xml");
        Files.writeString(pomPath, "<project></project>");

        var cmd = new SetCommand();
        cmd.pomPath = pomPath;
        cmd.properties = List.of(new SetCommand.Property("foo.bar", "1"), new SetCommand.Property("say", "hello"));
        cmd.run();

        var s = Files.readString(pomPath);
        var pat = Pattern.compile(".*<properties>\\s*<foo.bar>1</foo.bar>\\s*<say>hello</say>\\s*</properties>.*", Pattern.MULTILINE);
        var m = pat.matcher(s);
        assertNotNull(m);
        assertTrue(m.find());
    }

    @Test
    void shouldReplaceExisting() throws Exception {
        var pomPath = fs.getPath("pom.xml");
        Files.writeString(pomPath, "<project><properties><foo.bar>hello</foo.bar></properties></project>");

        var cmd = new SetCommand();
        cmd.pomPath = pomPath;
        cmd.properties = List.of(new SetCommand.Property("foo.bar", "world"));
        cmd.run();

        var s = Files.readString(pomPath);
        var pat = Pattern.compile(".*<properties>\\s*<foo.bar>world</foo.bar>\\s*</properties>.*", Pattern.MULTILINE);
        var m = pat.matcher(s);
        assertNotNull(m);
        assertTrue(m.find());
    }
}
