package com.github.andirady.pomcli;

import java.nio.file.Path;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
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
            throw new ExecutionException(spec.commandLine(), "Fail to read " + pomPath + ": " + e.getMessage());
        }
    }

    String readProjectId() throws Exception {
        var pomReader = new PomReader();
        var pom = pomReader.readPom(pomPath);
        return pom.getPackaging() + " " + pom.getGroupId() + ":" + pom.getArtifactId() + ":" + pom.getVersion();
    }

    private void updatePom() throws Exception {
        var pom = new Pom();
        var parts = id.split(":", 3);
        
        if (parts.length >= 2) {
            pom.setGroupId(parts[0]);
            pom.setArtifactId(parts[1]);
        } else if (parts.length == 1) {
            pom.setGroupId("unnamed");
            pom.setArtifactId(parts[0]);
        }

        if (parts.length >= 3) {
            pom.setVersion(parts[2]);
        } else {
            pom.setVersion("0.0.1-SNAPSHOT");
        }

        if (as != null) {
            pom.setPackaging(as);
        }

        var pomWriter = new PomWriter();
        pomWriter.write(pom, pomPath);
    }

}
