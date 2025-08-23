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

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NewPomTest extends BaseTest {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void shouldUsePomPathDirectoryName(boolean standalone) {
        var underTest = new NewPom();
        var model = underTest.newPom(Path.of("foo", "bar", "pom.xml"), standalone);
        assertEquals("bar", model.getArtifactId());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "<groupId>g</groupId>", "<version>1</version>" })
    void shouldFail(String content) throws IOException {
        var parentDir = createDirectories(getPath("b"));
        var parentPom = parentDir.resolve("pom.xml");
        writeString(parentPom, """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        %s
                        <artifactId>a</artifactId>
                    </parent>
                    <artifactId>b</artifactId>
                    <packaging>pom</packaging>
                </project>
                """.formatted(content));

        var dir = createDirectories(parentDir.resolve("c"));
        var underTest = new NewPom();
        assertThrows(Exception.class, () -> underTest.newPom(dir.resolve("pom.xml")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>a</groupId>
                        <artifactId>b</artifactId>
                        <version>1</version>
                        <packaging>pom</packaging>
                    </project>
                    """,
            """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>a</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                        </parent>
                        <artifactId>b</artifactId>
                        <packaging>pom</packaging>
                    </project>
                    """
    })
    void shouldHasGroupIdAndVersionInParent(String content) throws IOException {
        var parentDir = createDirectories(getPath("b"));
        var parentPom = parentDir.resolve("pom.xml");
        writeString(parentPom, content);

        var dir = createDirectories(parentDir.resolve("c"));
        var underTest = new NewPom();
        var actual = underTest.newPom(dir.resolve("pom.xml"));
        assertNotNull(actual.getParent());
        assertEquals("a", actual.getParent().getGroupId());
        assertEquals("b", actual.getParent().getArtifactId());
        assertEquals("1", actual.getParent().getVersion());
    }
}
