/**
 * Copyright 2021-2025 Andi Rady Kurniawan
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
import org.apache.maven.model.Parent;

import com.github.andirady.pomcli.converter.ParentConverter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = "parent", description = "Sets the parent for the project")
public class SetParentCommand extends ModifyingCommandBase {

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml")
    Path pomPath;

    @Parameters(arity = "1", paramLabel = "PARENT", converter = ParentConverter.class, description = "groupId:artifactId[:version] or path to a directory or pom.xml")
    Parent parent;

    @Option(names = { "-v", "--verbose" })
    boolean verbose;

    @Spec
    CommandSpec spec;

    @Override
    public int process(Model model) throws Exception {
        model.setParent(parent);

        if (verbose) {
            spec.commandLine().getOut().println("Parent is set to " + parent);
        }

        return 0;
    }

    @Override
    Path getPomPath() {
        return pomPath;
    }

}
