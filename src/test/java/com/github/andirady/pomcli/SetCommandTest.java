package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

class SetCommandTest extends BaseTest {

    private CommandLine underTest;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        underTest = Main.createCommandLine(new Main());
    }

    @AfterEach
    void cleanup() {
        deleteRecursive(tempDir);
    }

    @Test
    void failIfPathNotFound() {
        var pomPath = tempDir.resolve("pom.xml");

        var ec = underTest.execute("set", "-f", pomPath.toString(), "a=1");
        assertSame(1, ec);
    }

    @Test
    void single() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        Files.writeString(pomPath, "<project></project>");

        var ec = underTest.execute("set", "-f", pomPath.toString(), "foo.bar=1");
        assertSame(0, ec);

        var s = Files.readString(pomPath);
        var pat = Pattern.compile(".*<properties>\\s*<foo.bar>1</foo.bar>\\s*</properties>.*", Pattern.MULTILINE);
        var m = pat.matcher(s);
        assertNotNull(m);
        assertTrue(m.find());
    }

    @Test
    void multiple() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        Files.writeString(pomPath, "<project></project>");

        var ec = underTest.execute("set", "-f", pomPath.toString(), "foo.bar=1", "say=hello");
        assertSame(0, ec);

        var s = Files.readString(pomPath);
        var pat = Pattern.compile(".*<properties>\\s*<foo.bar>1</foo.bar>\\s*<say>hello</say>\\s*</properties>.*", Pattern.MULTILINE);
        var m = pat.matcher(s);
        assertNotNull(m);
        assertTrue(m.find());
    }

    @Test
    void replaceExisting() throws Exception {
        var pomPath = tempDir.resolve("pom.xml");
        Files.writeString(pomPath, "<project><properties><foo.bar>hello</foo.bar></properties></project>");

        underTest.execute("set", "-f", pomPath.toString(), "foo.bar=hello");

        var ec = underTest.execute("set", "-f", pomPath.toString(), "foo.bar=world");
        assertSame(0, ec);

        var s = Files.readString(pomPath);
        var pat = Pattern.compile(".*<properties>\\s*<foo.bar>world</foo.bar>\\s*</properties>.*", Pattern.MULTILINE);
        var m = pat.matcher(s);
        assertNotNull(m);
        assertTrue(m.find());
    }
}
