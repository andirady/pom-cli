package com.github.andirady.pomcli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;

import com.github.andirady.pomcli.converter.PluginConverter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = "plug")
public class PlugCommand implements Callable<Integer> {

    private static final Logger LOG = Logger.getLogger("plug");

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml")
    Path pomPath;

    @Parameters(arity = "1", paramLabel = "groupId:artifactId[:version]", description = "Plugin ID", converter = PluginConverter.class)
    Plugin plugin;

    @Spec
    CommandSpec spec;

    private Model model;

	@Override
	public Integer call() throws Exception {
        var reader = new DefaultModelReader();
        if (Files.exists(pomPath)) {
            try (var is = Files.newInputStream(pomPath)) {
                model = reader.read(is, null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            LOG.fine(() -> pomPath + " does not exists. Creating a new one");
            model = new NewPom().newPom(pomPath);
        }

        var added = new AddPlugin().addPlugin(model, plugin);

        var writer = new DefaultModelWriter();
		try (var os = Files.newOutputStream(pomPath)) {
            writer.write(os, null, model);
        } catch (FileNotFoundException e) {
            spec.commandLine().getOut().println("No such file: " + pomPath);
            return 1;
        }

        spec.commandLine().getOut().println(added + " added");

        return 0;
	}
}
