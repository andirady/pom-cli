/**
 * Copyright 2021-2026 Andi Rady Kurniawan
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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "unplug", description = "Removes a plugin")
public class UnplugCommand extends ModifyingCommandBase {

    @Option(names = { "-f",
            "--file" }, defaultValue = "pom.xml", order = 0, description = "Path to pom.xml. Can be a regular file or directory")
    Path pomPath;

    @Parameters(arity = "1", paramLabel = "[groupId:]artifactId", description = "Plugin ID")
    String pluginId;

    @Spec
    CommandSpec spec;

    @ParentCommand
    Main main;

    @Override
    Path getPomPath() {
        pomPath = pomPath.startsWith("~")
                ? Path.of(System.getProperty("user.home")).resolve(pomPath.subpath(1, pomPath.getNameCount()))
                : pomPath;

        if (Files.isDirectory(pomPath)) {
            pomPath = pomPath.resolve("pom.xml");
        }
        return pomPath;
    }

    @Override
    public int process(Model model) throws Exception {
        var target = main.getProfileId()
                .flatMap(id -> model.getProfiles().stream().filter(p -> p.getId().equals(id)).findFirst())
                .map(Profile::getBuild)
                .orElse(model.getBuild())
                .getPlugins().stream()
                .filter(p -> pluginId.indexOf(':') > 0
                        ? p.getId().startsWith(pluginId + ":")
                        : p.getId().matches("^.*?:" + pluginId + ":.*?$"))
                .findFirst()
                .orElse(null);

        if (!(target instanceof Plugin p)) {
            spec.commandLine().getErr().println("No plugin matched.");
            return 2;
        }

        model.getBuild().getPlugins().remove(p);
        spec.commandLine().getOut().printf("✅ %s unplugged%s%n", p.getId(),
                main.getProfileId().map(id -> " from profile %s".formatted(id)).orElse(""));

        return 0;
    }

    @Override
    boolean getPomPathMustExists() {
        return true;
    }

}
