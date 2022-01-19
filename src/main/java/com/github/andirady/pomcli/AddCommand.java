package com.github.andirady.pomcli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.PicocliException;
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
            try {
                model = reader.read(pomPath.toFile(), null);
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

        var deps = coords.stream().parallel().map(this::ensureVersion).toList();
		model.getDependencies().addAll(deps);
	
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

}
