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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class PropsCommandTest extends BaseTest {

    @Test
    void shouldListProperties() throws Exception {
        var pomPath = writeString(getTempPath().resolve("pom.xml"), """
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
        var cli = Main.createCommandLine(new Main());
        cli.setOut(new PrintWriter(out));

        var exitCode = cli.execute("props", "--file", pomPath.toString());

        assertEquals(0, exitCode);
        assertEquals("foo=hello", out.toString().trim());
    }
}
