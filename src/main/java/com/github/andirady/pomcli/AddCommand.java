package com.github.andirady.pomcli;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "add", sortOptions = false)
public class AddCommand implements Runnable {

    private static final Logger LOG = Logger.getLogger("add");

    static class Scope {

        @Option(names = "--compile", description = "Add as compile dependency. This is the default", order = 0)
        boolean compile;

        @Option(names = "--runtime", description = "Add as runtime dependency", order = 1)
        boolean runtime;

        @Option(names = "--provided", description = "Add as provided dependency", order = 2)
        boolean provided;

        @Option(names = "--test", description = "Add as test dependency", order = 3)
        boolean test;

        String value() {
            if (runtime)  return "runtime";
            if (provided) return "provide";
            if (test)     return "test";

            return "compile";
        }

    }

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml", order = 0)
    Path pomPath;

    @ArgGroup(exclusive = true, multiplicity = "0..1", order = 1)
    Scope scope;

    @Parameters(arity = "1..*", paramLabel = "groupId:artifactId[:version]")
    List<Dependency> coords;

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        Model model;
        if (Files.exists(pomPath)) {
            var reader = new DefaultModelReader();
            try (var is = Files.newInputStream(pomPath)) {
                model = reader.read(is, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.fine(() -> pomPath + " does not exists. Creating a new one");
            model = new Model();
            model.setModelVersion("4.0.0");
            model.setGroupId("unnamed");
            model.setArtifactId(Path.of(System.getProperty("user.dir")).getFileName().toString());
            model.setVersion("0.0.1-SNAPSHOT");
        }

        var existing = model.getDependencies();
        var duplicates = coords.stream()
                               .filter(c -> existing.stream().anyMatch(d -> sameArtifact(c, d)))
                               .map(this::coordString)
                               .collect(joining(", "));
        if (duplicates.length() > 0) {
            throw new IllegalArgumentException("Duplicate artifact(s): " + duplicates);
        }

        var stream = coords.stream().parallel().map(this::ensureVersion);
        if (scope != null && !"compile".equals(scope.value())) {
            stream = stream.map(d -> {
                d.setScope(scope.value());
                return d;
            });
        }

        var deps = stream.toList();
        existing.addAll(deps);
	
        var writer = new DefaultModelWriter();
		try (var os = Files.newOutputStream(pomPath)) {
            writer.write(os, null, model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	Dependency ensureVersion(Dependency d) {
		if (d.getVersion() == null) {
            var latestVersion = new GetLatestVersion().execute(new QuerySpec(d.getGroupId(), d.getArtifactId(), null));
            d.setVersion(latestVersion.orElseThrow(() -> new ExecutionException(
                    spec.commandLine(), "No version found: '" + d.getGroupId() + ":" + d.getArtifactId() + "'")
                ));
		}

        return d;
	}

    boolean sameArtifact(Dependency d1, Dependency d2) {
        var g = d1.getGroupId().equals(d2.getGroupId());
        var a = d1.getArtifactId().equals(d2.getArtifactId());
        var classfifier = (d1.getClassifier() != null && d1.getClassifier().equals(d2.getClassifier()))
                || (d1.getClassifier() == d2.getClassifier()); // null
        return g && a && classfifier;
    }

    String coordString(Dependency d) {
        return d.getGroupId() + ":"
             + d.getArtifactId()
             + Optional.ofNullable(d.getClassifier()).map(c -> ":" + c).orElse("");
    }

}
