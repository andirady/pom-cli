package com.github.andirady.pomcli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.logging.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "id")
public class IdCommand implements Runnable {

    private static final Logger LOG = Logger.getLogger(IdCommand.class.getName());

    @Option(names = { "--as" })
    String as;

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml")
    Path pomPath;

    @Parameters(arity = "0..1", paramLabel = "groupId:artifactId[:version]", description = {"Project id"})
    String id;

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        if (id != null) {
            updatePom();
        } else if (Files.notExists(pomPath)) {
            throw new IllegalStateException("No such file: " + pomPath);
        }

        System.out.println(readProjectId());
    }

    String readProjectId() {
        var pomReader = new DefaultModelReader();
        Model pom;
        try (var is = Files.newInputStream(pomPath)) {
            pom = pomReader.read(is, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var g = pom.getGroupId();
        var v = pom.getVersion();
        var parent = pom.getParent();
        if (parent != null) {
            if (g == null) {
                g = parent.getGroupId();
            }
            if (v == null) {
                v = parent.getVersion();
            }
        }
        return pom.getPackaging() + " " + g + ":" + pom.getArtifactId() + ":" + v;
    }

    private void updatePom() {
        Model pom;
        var reader = new DefaultModelReader();
        if (Files.exists(pomPath)) {
            LOG.fine(() -> "Reading existing pom at " + pomPath);
            try (var is = Files.newInputStream(pomPath)) {
                pom = reader.read(is, null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            LOG.fine(() -> "Creating new pom at " + pomPath);
            pom = new NewPom().newPom(pomPath);
        }

        parseId(id, pom);

        if (pom.getParent() == null) {
            if (pom.getGroupId() == null) {
                pom.setGroupId("unnamed");
            }
            if (pom.getVersion() == null) {
                pom.setVersion("0.0.1-SNAPSHOT");
            }
        }

        if (as != null) {
            pom.setPackaging(as);
        }

        var pomWriter = new DefaultModelWriter();
        try (var os = Files.newOutputStream(pomPath)) {
            pomWriter.write(os, null, pom);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void parseId(String id, Model pom) {
        var parts = id.split(":", 3);
        
        if (parts.length >= 2) {
            pom.setGroupId(parts[0]);
            pom.setArtifactId(parts[1]);
        } else if (parts.length == 1) {
            pom.setArtifactId(parts[0]);
        }

        if (parts.length >= 3) {
            pom.setVersion(parts[2]);
        }
    }

}
