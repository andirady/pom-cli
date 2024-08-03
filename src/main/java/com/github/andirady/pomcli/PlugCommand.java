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

import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;

import com.github.andirady.pomcli.converter.PluginConverter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "plug")
public class PlugCommand extends ModifyingCommandBase {

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml")
    Path pomPath;

    @Parameters(arity = "1", paramLabel = "groupId:artifactId[:version]", description = "Plugin ID", converter = PluginConverter.class)
    Plugin plugin;

    @Spec
    CommandSpec spec;

    @ParentCommand
    Main main;

    @Override
    public int process(Model model) throws Exception {
        var added = new AddPlugin(main.getProfileId().orElse(null)).addPlugin(model, plugin);
        spec.commandLine().getOut().println("🔌 " + added.getId() + " plugged");

        return 0;
    }

    @Override
    Path getPomPath() {
        return pomPath;
    }

}
