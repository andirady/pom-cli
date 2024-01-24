package com.github.andirady.pomcli;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelReader;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "add", sortOptions = false)
public class AddCommand implements Runnable {

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

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml", order = 0)
    Path pomPath;

    @ArgGroup(exclusive = true, multiplicity = "0..1", order = 1)
    Scope scope;

    @Parameters(arity = "1..*", paramLabel = "DEPENDENCY", description = """
            groupId:artifactId[:version] or path to either
            a directory, pom.xml, or a jar file.""")
    List<Dependency> coords;

    @Spec
    CommandSpec spec;

    private Model model;
    private Model parentPom;

    @Override
    public void run() {
        var reader = new DefaultModelReader(null);
        if (Files.exists(pomPath)) {
            try (var is = Files.newInputStream(pomPath)) {
                model = reader.read(is, null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            System.err.println(pomPath + " does not exists. Creating a new one");
            model = new NewPom().newPom(pomPath);
        }

        readParentPom(reader);

        var existing = getExistingDependencies();
        var duplicates = coords.stream()
                .filter(c -> existing.stream().anyMatch(d -> sameArtifact(c, d, false)))
                .map(this::coordString)
                .collect(joining(", "));
        if (duplicates.length() > 0) {
            throw new IllegalArgumentException("Duplicate artifact(s): " + duplicates);
        }

        var stream = coords.stream().parallel().map(this::ensureVersion);
        if (scope != null && !scope.compile) {
            stream = stream.map(d -> {
                d.setScope(scope.value());
                if (scope.importScope) {
                    d.setType("pom");
                }
                return d;
            });
        }

        var deps = stream.toList();
        existing.addAll(deps);

        var writer = new DefaultModelWriter();
        try (var os = Files.newOutputStream(pomPath)) {
            writer.write(os, null, model);
            deps.forEach(d -> System.out.printf(
                    "%s %s%s added%n",
                    switch (Objects.requireNonNullElse(d.getScope(), "compile")) {
                        case "provided" -> "📦";
                        case "runtime" -> "🏃";
                        case "test" -> "🔬";
                        case "import" -> "🚢";
                        default -> "🔨";
                    },
                    coordString(d),
                    Optional.ofNullable(d.getVersion()).map(":"::concat).orElse("")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void readParentPom(ModelReader reader) {
        var parent = model.getParent();
        if (parent == null) {
            return;
        }

        var parentRelativePath = parent.getRelativePath();
        var parentPomPath = pomPath.toAbsolutePath().getParent().resolve(parentRelativePath);
        var filename = "pom.xml";
        if (!parentPomPath.getFileName().toString().equals(filename)) {
            parentPomPath = parentPomPath.resolve(filename);
        }

        // If the parent pom doesn't exists, tread the parent as remote parent.
        if (!Files.exists(parentPomPath)) {
            var system = ServiceLoader.load(RepositorySystemSupplier.class).findFirst()
                    .orElseThrow(() -> new NoSuchElementException("No provider for " + RepositorySystemSupplier.class.getName()))
                    .get();
            var artifact = new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), null, "pom",
                    parent.getVersion());
            List<RemoteRepository> repositories = List
                    .of(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/")
                            .build());
            var sessionBuilder = new SessionBuilderSupplier(system).get()
                    .withLocalRepositoryBaseDirectories(
                            Path.of(System.getProperty("user.home"), ".m2", "repository").toFile());
            try (
                var session = sessionBuilder.build()
            ) {
                var artifactRequest = new ArtifactRequest(artifact, repositories, null);
                var artifactResult = system.resolveArtifact(session, artifactRequest);
                parentPomPath = artifactResult.getArtifact().getFile().toPath();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        try (var is = Files.newInputStream(parentPomPath)) {
            parentPom = reader.read(is, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

        if (parentPom != null) {
            if (parentPom.getDependencyManagement() instanceof DependencyManagement dm && dm.getDependencies()
                    .stream()
                    .filter(d -> sameArtifact(d, dep, true))
                    .findFirst()
                    .orElse(null) instanceof Dependency managed) {
                if (dep.getGroupId() == null) {
                    dep.setGroupId(managed.getGroupId());
                }
                return dep;
            }

            var remotelyManaged = ServiceLoader.load(ResolutionProvider.class)
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("No provider for " + ResolutionProvider.class.getName()))
                    .findByArtifactId(parentPom, dep.getArtifactId(), scope instanceof Scope s ? s.value() : "compile")
                    .orElse(null);
            if (remotelyManaged != null) {
                // Immediately return if the groupId already set.
                if (dep.getGroupId() instanceof String s && s.equals(remotelyManaged.getGroupId())) {
                    return dep;
                }

                dep.setGroupId(remotelyManaged.getGroupId());
                return dep;
            }
        }

        var latestVersion = new GetLatestVersion()
                .execute(new QuerySpec(dep.getGroupId(), dep.getArtifactId(), null))
                .orElseThrow(() -> new IllegalStateException(
                        "No version found: '" + coordString(dep) + "'"));
        dep.setVersion(latestVersion);

        return dep;
    }

    boolean sameArtifact(Dependency d1, Dependency d2, boolean ignoreGroupId) {
        if (!ignoreGroupId && !Objects.equals(d1.getGroupId(), d2.getGroupId())) {
            return false;
        }

        return d1.getArtifactId().equals(d2.getArtifactId()) && Objects.equals(d1.getClassifier(), d2.getClassifier());
    }

    String coordString(Dependency d) {
        return d.getGroupId() + ":"
                + d.getArtifactId()
                + Optional.ofNullable(d.getClassifier()).map(":"::concat).orElse("");
    }

}
