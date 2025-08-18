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

import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class GetCommandTest extends BaseTest {

    CommandLine underTest;
    private Path projectPath;

    @BeforeEach
    void setup() {
        var app = new Main();
        underTest = new CommandLine(app);
        projectPath = getTempPath();
    }

    @Test
    void shouldReturnProperty() throws IOException {
        var pomPath = writeString(projectPath.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>a</groupId>
                  <artifactId>a</artifactId>
                  <version>1</version>
                  <properties>
                    <foo>hello</foo>
                  </properties>
                </project>
                """);

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));
        underTest.execute("get", "-f", pomPath.toString(), "foo");

        assertEquals("hello", out.toString().trim());
    }

    @Test
    void shouldReturnPropertyFromProfile() throws IOException {
        var pomPath = writeString(projectPath.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>a</groupId>
                  <artifactId>a</artifactId>
                  <version>1</version>
                  <properties>
                    <foo>hello</foo>
                  </properties>
                  <profiles>
                    <profile>
                      <id>test</id>
                      <properties>
                        <foo>world</foo>
                      </properties>
                    </profile>
                  </profiles>
                </project>
                """);

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));
        underTest.execute("--profile", "test", "get", "-f", pomPath.toString(), "foo");

        assertEquals("world", out.toString().trim());
    }

    @AfterEach
    void cleanup() throws IOException {
        deleteRecursive(projectPath);
    }
}
