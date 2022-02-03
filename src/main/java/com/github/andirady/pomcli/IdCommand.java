package com.github.andirady.pomcli;

import java.nio.file.Files;
import java.nio.file.Path;

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
        try {
            if (id != null) {
                updatePom();
            }

            System.out.println(readProjectId());
        } catch (Exception e) {
            throw new RuntimeException("Fail to read " + pomPath + ": " + e.getMessage(), e);
        }
    }

    String readProjectId() throws Exception {
        var pomReader = new DefaultModelReader();
        Model pom;
        try (var is = Files.newInputStream(pomPath)) {
            pom = pomReader.read(is, null);
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

    private void updatePom() throws Exception {
        Model pom;
        var reader = new DefaultModelReader();
        if (Files.exists(pomPath)) {
            try (var is = Files.newInputStream(pomPath)) {
                pom = reader.read(is, null);
            }
        } else {
            pom = new NewPom().newPom(pomPath);
        }

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
        }
    }

}
