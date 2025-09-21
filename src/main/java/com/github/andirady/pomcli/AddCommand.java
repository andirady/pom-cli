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

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelReader;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "add", sortOptions = false, description = "Add dependencies")
public class AddCommand extends ReadingOptions implements Runnable {

    private static final Logger LOG = Logger.getLogger("add");

    static class Scope {

        @Option(names = { "-c",
                "--compile" }, description = "Add as compile dependency. This is the default", order = 0)
        boolean compile;

        @Option(names = { "-r", "--runtime" }, description = "Add as runtime dependency", order = 1)
        boolean runtime;

        @Option(names = { "-p", "--provided" }, description = "Add as provided dependency", order = 2)
        boolean provided;

        @Option(names = { "-t", "--test" }, description = "Add as test dependency", order = 3)
        boolean test;

        @Option(names = { "-i", "--import" }, description = "Add as import dependency", order = 4)
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

    @ArgGroup(exclusive = true, multiplicity = "0..1", order = 1)
    Scope scope;

    @Option(names = { "-o", "--optional" }, defaultValue = "false")
    public boolean optional;

    @Parameters(arity = "1..*", paramLabel = "DEPENDENCY", description = """
            groupId:artifactId[:version] or path to either
            a directory, pom.xml, or a jar file.""")
    List<Dependency> coords;

    @Option(names = { "-e",
            "--excludes" }, split = ",", defaultValue = "", paramLabel = "[GROUP_ID:]ARTIFACT_ID[:VERSION]", description = """
                    Comma-separated list of exclusions
                    """)
    List<String> excludes;

    @Spec
    CommandSpec spec;

    private Model model;
    private boolean isRemoteParent;
    private Model parentPom;

    @Override
    public void run() {
        model = getPom().orElseGet(() -> {
            spec.commandLine().getErr().println(pomPath + " does not exists. Creating a new one");
            return new NewPom().newPom(getPomFilePath());
        });
        readParentPom(getPomReader());

        var existing = getExistingDependencies();
        LOG.fine("Checking for duplicates");
        var duplicates = coords.stream()
                .filter(c -> existing.stream().anyMatch(d -> sameArtifact(c, d, c.getGroupId() == null)))
                .map(this::coordString)
                .collect(joining(", "));
        if (duplicates.length() > 0) {
            LOG.fine(() -> String.format("Found %s duplicates", duplicates.length()));
            throw new IllegalArgumentException("Duplicate artifact(s): " + duplicates);
        }

        var stream = coords.stream().parallel().map(this::ensureVersion).map(this::addExclusions);

        // Add the scope element if the scope is not compile scope.
        if (scope != null && !scope.compile) {
            stream = stream.map(d -> {
                d.setScope(scope.value());
                if (scope.importScope) {
                    d.setType("pom");
                }
                return d;
            });
        }

        if (optional) {
            if ("pom".equals(model.getPackaging())) {
                throw new UnsupportedOperationException(
                        "Adding optional dependency with `pom' packaging is not supported");
            }
            stream = stream.map(d -> {
                d.setOptional(optional);
                return d;
            });
        }

        var deps = stream.toList();
        existing.addAll(deps);

        var writer = new DefaultModelWriter();
        try (var os = Files.newOutputStream(getPomFilePath())) {
            writer.write(os, null, model);
            deps.forEach(d -> System.out.printf(
                    "%s %s%s added%s%n",
                    switch (d.getScope()) {
                        case "provided" -> "📦";
                        case "runtime" -> "🏃";
                        case "test" -> "🔬";
                        case "import" -> "🚢";
                        case null, default -> "🔨";
                    },
                    coordString(d),
                    d.getVersion() instanceof String version
                            ? ":" + version
                            : Ansi.AUTO.string(":@|italic,faint <managed>|@"),
                    d.isOptional() ? Ansi.AUTO.string(" [@|yellow optional|@]") : ""));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void readParentPom(ModelReader reader) {
        new ParentPomFinder(reader).find(pomPath, model).ifPresent(result -> {
            isRemoteParent = result.remote();
            parentPom = result.pom();
        });
    }

    List<Dependency> getExistingDependencies() {
        if (!"pom".equals(model.getPackaging()) && (scope == null || !scope.importScope)) {
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

    Dependency ensureVersion(Dependency dep) {
        if (dep.getVersion() != null) {
            return dep;
        }

        var scopeName = scope instanceof Scope s ? s.value() : "compile";
        var resolver = ResolutionProvider.getInstance();

        if (streamManaged(model)
                .filter(this::isImportScope)
                .map(d -> resolver.readModel(d.getGroupId(), d.getArtifactId(), d.getVersion()))
                .flatMap(m -> resolver.findByArtifactId(m, dep.getGroupId(), dep.getArtifactId(), scopeName).stream())
                .findFirst().orElse(null) instanceof Dependency managed) {
            if (dep.getGroupId() == null) {
                dep.setGroupId(managed.getGroupId());
            }
            return dep;
        }

        if (parentPom != null) {
            if (streamManaged(parentPom)
                    .filter(d -> sameArtifact(d, dep, true))
                    .findFirst()
                    .orElse(null) instanceof Dependency managed) {
                if (dep.getGroupId() == null) {
                    dep.setGroupId(managed.getGroupId());
                }
                return dep;
            }

            var remotePom = isRemoteParent
                    ? parentPom
                    : parentPom.getParent() instanceof Parent p
                            ? resolver.readModel(p.getGroupId(), p.getArtifactId(), p.getVersion())
                            : null;
            if (remotePom != null) {
                var remotelyManaged = resolver
                        .findByArtifactId(remotePom, dep.getGroupId(), dep.getArtifactId(), scopeName)
                        .orElse(null);
                if (remotelyManaged != null) {
                    if (dep.getGroupId() == null) {
                        dep.setGroupId(remotelyManaged.getGroupId());
                    }

                    return dep;
                }
            }
        }

        var latestVersion = new GetLatestVersion()
                .execute(new QuerySpec(dep.getGroupId(), dep.getArtifactId(), null))
                .orElseThrow(() -> new IllegalStateException(
                        "No version found: '" + coordString(dep) + "'"));
        dep.setVersion(latestVersion);

        return dep;
    }

    Stream<Dependency> streamManaged(Model model) {
        return model.getDependencyManagement() instanceof DependencyManagement dm
                ? dm.getDependencies().stream()
                : Stream.empty();
    }

    boolean sameArtifact(Dependency d1, Dependency d2, boolean ignoreGroupId) {
        LOG.fine(() -> String.format("Comparing %s with %s", d1, d2));
        if (!ignoreGroupId && !Objects.equals(d1.getGroupId(), d2.getGroupId())) {
            return false;
        }

        return d1.getArtifactId().equals(d2.getArtifactId()) && Objects.equals(d1.getClassifier(), d2.getClassifier());
    }

    boolean isImportScope(Dependency dependency) {
        return "pom".equals(dependency.getType()) && "import".equals(dependency.getScope());
    }

    String coordString(Dependency d) {
        return d.getGroupId() + ":"
                + d.getArtifactId()
                + Optional.ofNullable(d.getClassifier()).map(":"::concat).orElse("");
    }

    Dependency addExclusions(Dependency dependency) {
        excludes.stream().forEach(a -> {
            var d = Main.stringToDependency(a);
            var e = new Exclusion();

            if (d.getGroupId() instanceof String g) {
                e.setGroupId(g);
            } else {
                e.setGroupId("*"); // wildcard exclusion. See MNG-3832
            }
            e.setArtifactId(d.getArtifactId());

            dependency.addExclusion(e);
        });

        return dependency;
    }

}
