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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelWriter;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "move", aliases = "mv", sortOptions = false, description = "Move dependencies to a different scope")
public class MoveCommand extends ReadingOptions implements Runnable {

    static class Scope {

        @Option(names = { "-c", "--compile" }, description = "Move to compile scope", order = 0)
        boolean compile;

        @Option(names = { "-r", "--runtime" }, description = "Move to runtime scope", order = 1)
        boolean runtime;

        @Option(names = { "-p", "--provided" }, description = "Move to provided scope", order = 2)
        boolean provided;

        @Option(names = { "-t", "--test" }, description = "Move to test scope", order = 3)
        boolean test;

        @Option(names = { "-i", "--import" }, description = "Move to import scope", order = 4)
        boolean importScope;

        String value() {
            if (runtime)
                return "runtime";
            if (provided)
                return "provided";
            if (test)
                return "test";
            if (importScope)
                return "import";

            return "compile";
        }
    }

    @ArgGroup(exclusive = true, multiplicity = "1", order = 1)
    Scope scope;

    @Parameters(arity = "1..*", paramLabel = "DEPENDENCY")
    List<Dependency> coords;

    private Model model;

    @Override
    public void run() {
        model = getPom().orElseThrow(() -> new IllegalStateException(pomPath + " is not a file."));

        var dependencies = coords.stream()
                .map(this::findExisting)
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
        coords.stream()
                .filter(d -> findExisting(d).isEmpty())
                .forEach(d -> ansiPrint("@|yellow,bold " + format(d) + "|@ @|yellow is not a dependency|@"));

        if (dependencies.isEmpty()) {
            return;
        }

        var targetDependencies = targetDependencies();
        for (var dependency : dependencies) {
            if (!targetDependencies.contains(dependency) && targetDependencies.stream().anyMatch(d -> sameArtifact(d, dependency))) {
                throw new IllegalArgumentException("Duplicate artifact: " + format(dependency));
            }
        }

        dependencies.forEach(this::move);

        var writer = new DefaultModelWriter();
        try (var os = Files.newOutputStream(pomPath)) {
            writer.write(os, Map.of(), model);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void move(Dependency dependency) {
        switch (scope.value()) {
            case "compile" -> {
                dependency.setScope(null);
                if ("pom".equals(dependency.getType())) {
                    dependency.setType(null);
                }
            }
            case "import" -> {
                dependency.setScope("import");
                dependency.setType("pom");
            }
            default -> {
                dependency.setScope(scope.value());
                if ("pom".equals(dependency.getType())) {
                    dependency.setType(null);
                }
            }
        }

        var targetDependencies = targetDependencies();
        if (!targetDependencies.contains(dependency)) {
            removeFromCurrentLocation(dependency);
            targetDependencies.add(dependency);
        }

        ansiPrint("@|bold " + format(dependency) + "|@ moved to @|bold " + scope.value() + "|@");
    }

    private Optional<Dependency> findExisting(Dependency dependency) {
        return allDependencies().stream()
                .filter(d -> d.getArtifactId().equals(dependency.getArtifactId()))
                .filter(d -> dependency.getGroupId() instanceof String groupId ? groupId.equals(d.getGroupId()) : true)
                .findFirst();
    }

    private List<Dependency> allDependencies() {
        var result = new ArrayList<Dependency>();
        result.addAll(model.getDependencies());
        if (model.getDependencyManagement() instanceof DependencyManagement dm) {
            result.addAll(dm.getDependencies());
        }
        return result;
    }

    private List<Dependency> targetDependencies() {
        if (!"pom".equals(model.getPackaging()) && !"import".equals(scope.value())) {
            return model.getDependencies();
        }

        var dm = model.getDependencyManagement();
        if (dm == null) {
            dm = new DependencyManagement();
            dm.setDependencies(new ArrayList<>());
            model.setDependencyManagement(dm);
        }

        return dm.getDependencies();
    }

    private void removeFromCurrentLocation(Dependency dependency) {
        model.getDependencies().remove(dependency);
        if (model.getDependencyManagement() instanceof DependencyManagement dm) {
            dm.getDependencies().remove(dependency);
        }
    }

    private boolean sameArtifact(Dependency d1, Dependency d2) {
        return Objects.equals(d1.getGroupId(), d2.getGroupId())
                && Objects.equals(d1.getArtifactId(), d2.getArtifactId())
                && Objects.equals(d1.getClassifier(), d2.getClassifier());
    }

    private String format(Dependency dependency) {
        return (dependency.getGroupId() instanceof String g ? (g + ":") : "") + dependency.getArtifactId()
                + (dependency.getVersion() instanceof String v ? (":" + v) : "");
    }

    private void ansiPrint(String message) {
        System.out.println(Ansi.AUTO.string(message));
    }
}
