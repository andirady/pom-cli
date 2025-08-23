package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NewPomTest {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void shouldUsePomPathDirectoryName(boolean standalone) {
        var underTest = new NewPom();
        var model = underTest.newPom(Path.of("foo", "bar", "pom.xml"), standalone);
        assertEquals("bar", model.getArtifactId());
    }
}
