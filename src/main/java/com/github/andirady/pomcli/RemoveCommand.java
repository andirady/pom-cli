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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi;

@Command(name = "remove", aliases = "rm", description = "Remove a dependency")
public class RemoveCommand implements Runnable {

    private static final Logger LOG = Logger.getLogger("remove");

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml", order = 0)
    Path pomPath;

    @Parameters(arity = "1..*", paramLabel = "DEPENDENCY")
    List<Dependency> coords;

    private Model model;

    @Override
    public void run() {
        var reader = new DefaultModelReader(null);
        try (var is = Files.newInputStream(pomPath)) {
            model = reader.read(is, Map.of());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var dependencies = coords.stream().map(this::findExisting).filter(Optional::isPresent)
                .map(Optional::orElseThrow).toList();
        coords.stream().filter(d -> findExisting(d).isEmpty())
                .forEach(d -> ansiPrint("@|yellow,bold " + format(d) + "|@ @|yellow is not a dependency|@"));

        if (dependencies.isEmpty()) {
            LOG.fine("No dependencies removed");
            return;
        }

        model.getDependencies().removeIf(dependencies::contains);
        dependencies.forEach(d -> ansiPrint("@|bold " + format(d) + "|@ removed"));

        var writer = new DefaultModelWriter();
        try (var os = Files.newOutputStream(pomPath)) {
            writer.write(os, Map.of(), model);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Optional<Dependency> findExisting(Dependency dependency) {
        return model.getDependencies().stream()
                .filter(d -> d.getArtifactId().equals(dependency.getArtifactId()))
                .filter(d -> dependency.getGroupId() instanceof String groupId ? groupId.equals(d.getGroupId()) : true)
                .findFirst();
    }

    private String format(Dependency dependency) {
        return (dependency.getGroupId() instanceof String g ? (g + ":") : "") + dependency.getArtifactId()
                + (dependency.getVersion() instanceof String v ? (":" + v) : "");
    }

    private void ansiPrint(String message) {
        System.out.println(Ansi.AUTO.string(message));
    }

}
