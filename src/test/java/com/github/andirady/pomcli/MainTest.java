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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void shouldPrintVersion() {
        var out = new StringWriter();
        var underTest = Main.createCommandLine(new Main());
        underTest.setOut(new PrintWriter(out));

        var ec = underTest.execute("--version");

        assertSame(0, ec);
        assertTrue(out.toString().trim().matches("pom-cli \\S+"));
    }

}
