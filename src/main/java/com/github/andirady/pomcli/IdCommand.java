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
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Help.Ansi;

@Command(name = "id")
public class IdCommand implements Callable<Integer> {

    private static final Logger LOG = Logger.getLogger(IdCommand.class.getName());

    @Option(names = { "--as" })
    String as;

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml")
    Path pomPath;

    @Option(names = { "-s", "--standalone" }, description = "Don't search for parent pom")
    boolean standalone;

    @Parameters(arity = "0..1", paramLabel = "groupId:artifactId[:version]", description = { "Project id" })
    String id;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        if (id != null) {
            updatePom();
        } else if (Files.notExists(pomPath)) {
            spec.commandLine().getOut()
                    .println(Ansi.AUTO.string("@|bold,fg(red) No such file:|@ @|fg(red) " + pomPath + "|@"));
            return 1;
        }

        spec.commandLine().getOut().println(readProjectId());

        return 0;
    }

    String readProjectId() {
        var pomReader = new DefaultModelReader(null);
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
        var reader = new DefaultModelReader(null);
        if (Files.exists(pomPath)) {
            LOG.fine(() -> "Reading existing pom at " + pomPath);
            try (var is = Files.newInputStream(pomPath)) {
                pom = reader.read(is, null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            LOG.fine(() -> "Creating new pom at " + pomPath);
            pom = new NewPom().newPom(pomPath, standalone);
        }

        parseId(id, pom);

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
        } else if (pom.getParent() != null) {
            // Ensure version is not set if <parent> is present
            pom.setVersion(null);
        }

        if (".".equals(pom.getArtifactId())) {
            pom.setArtifactId(pomPath.toAbsolutePath().getParent().getFileName().toString());
        }
    }

}
