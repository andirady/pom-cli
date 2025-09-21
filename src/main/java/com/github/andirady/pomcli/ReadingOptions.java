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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;

import picocli.CommandLine.Option;

public class ReadingOptions {

    private static ModelReader modelReader;

    @Option(names = { "-f",
            "--file" }, defaultValue = "pom.xml", order = 0, description = "The path to pom.xml or it's directory")
    protected Path pomPath;

    public Optional<Model> getPom() {
        var f = getPomFilePath();
        if (Files.notExists(f)) {
            return Optional.empty();
        }

        try (var is = Files.newInputStream(f)) {
            return Optional.of(getPomReader().read(is, null));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected Path getPomFilePath() {
        return Files.isDirectory(pomPath) ? pomPath.resolve("pom.xml") : pomPath;
    }

    protected ModelReader getPomReader() {
        if (modelReader == null) {
            synchronized (this) {
                if (modelReader == null) {
                    modelReader = new DefaultModelReader(null);
                }
            }
        }
        return modelReader;
    }
}
