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
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "unset")
public class UnsetCommand implements Runnable {

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml")
    Path pomPath;

    @Parameters(arity = "1")
    String property;

    @Spec
    CommandSpec spec;

    @ParentCommand
    Main main;

    @Override
    public void run() {
        var pomReader = new DefaultModelReader(null);
        Model pom;
        try (var is = Files.newInputStream(pomPath)) {
            pom = pomReader.read(is, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var profileId = main.getProfileId();
        var propertyValue = profileId.map(id -> getPropertiesForProfile(pom, id))
                .orElseGet(pom::getProperties)
                .remove(property);

        Objects.requireNonNull(propertyValue,
                "No such property: " + property + profileId.map(i -> ", profile: " + i).orElse(""));

        spec.commandLine().getOut().println("✅ Property `%s' unset, the value was `%s'".formatted(property, propertyValue));

        var pomWriter = new DefaultModelWriter();
        try (var out = Files.newOutputStream(pomPath)) {
            pomWriter.write(out, Map.of(), pom);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Properties getPropertiesForProfile(Model pom, String profileId) {
        return pom.getProfiles()
                .stream()
                .filter(p -> profileId.equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such profile: " + profileId))
                .getProperties();
    }

}
