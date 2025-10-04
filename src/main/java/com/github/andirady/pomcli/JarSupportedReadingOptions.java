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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class JarSupportedReadingOptions extends ReadingOptions {

    @Override
    protected Path getPomFilePath() {
        if (pomPath.toString().endsWith(".jar")) {
            try {
                return getPomPathFromJar(pomPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return super.getPomFilePath();
    }

    protected Path getPomPathFromJar(Path pomPath) throws IOException {
        var zipfs = FileSystems.newFileSystem(pomPath);
        return Files.walk(zipfs.getPath("META-INF", "maven"))
                .filter(p -> p.getFileName().toString().equals("pom.xml"))
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IOException("No maven metadata"));

    }

}
