/**
 * Copyright 2021-2024 Andi Rady Kurniawan
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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;

class RemoveCommandTest extends BaseTest {

    CommandLine underTest;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        var app = new Main();
        underTest = Main.createCommandLine(app);
    }

    @Test
    void removeNonDependency() throws IOException {
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

        var ec = underTest.execute("rm", "-f", pomPath.toString(), "b");
        assertSame(0, ec);
        assertXpath(pomPath, "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1']", 1);
    }

    @ParameterizedTest
    @ValueSource(strings = { "g:a:1", "g:a", "a" })
    void success(String coord) throws Exception {
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

        var ec = underTest.execute("rm", "-f", pomPath.toString(), coord);
        assertSame(0, ec);
        assertXpath(pomPath, "/project/dependencies/dependency[groupId='g' and artifactId='a' and version='1']", 0);
    }
}
