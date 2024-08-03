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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;

import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "set")
public class SetCommand implements Runnable {

    record Property(String key, String value) {
    }

    public static class PropertyConverter implements ITypeConverter<Property> {
        public Property convert(String value) throws Exception {
            var parts = value.split("=", 2);
            return new Property(parts[0], parts[1]);
        }
    }

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml", order = 0)
    Path pomPath;

    @Parameters(arity = "1..*", paramLabel = "key=value", converter = PropertyConverter.class)
    List<Property> properties;

    @Override
    public void run() {
        if (!Files.isRegularFile(pomPath)) {
            throw new IllegalStateException(pomPath + " is not a file.");
        }

        var reader = new DefaultModelReader(null);
        Model pom;
        try (var is = Files.newInputStream(pomPath)) {
            pom = reader.read(is, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var props = pom.getProperties();
        for (var p : properties) {
            props.setProperty(p.key(), p.value());
        }

        var writer = new DefaultModelWriter();
        try (var os = Files.newOutputStream(pomPath)) {
            writer.write(os, null, pom);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
