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

import java.util.Objects;

import org.apache.maven.model.Profile;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "get", description = "Get properties")
public class GetCommand extends ReadingOptions implements Runnable {

    @Parameters(arity = "1")
    String property;

    @Spec
    CommandSpec spec;

    @ParentCommand
    Main main;

    @Override
    public void run() {
        var pom = getPom().orElseThrow(() -> new IllegalStateException(pomPath + " is not a file."));
        var profileId = main.getProfileId();
        var propertyValue = profileId.map(id -> pom.getProfiles()
                .stream()
                .filter(p -> id.equals(p.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such profile: " + profileId)))
                .map(Profile::getProperties)
                .map(p -> p.getProperty(property))
                .orElseGet(() -> pom.getProperties().getProperty(property));
        spec.commandLine().getOut()
                .println(Objects.requireNonNull(propertyValue,
                        "No such property: " + property + profileId.map(i -> ", profile: " + i).orElse("")));
    }

}
