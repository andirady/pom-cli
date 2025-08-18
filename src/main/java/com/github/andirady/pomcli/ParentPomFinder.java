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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.ModelReader;

public class ParentPomFinder {

    public record Result(Model pom, boolean remote) {
    }

    private static final Logger LOG = Logger.getLogger("");

    private ModelReader reader;

    public ParentPomFinder(ModelReader reader) {
        this.reader = reader;
    }

    Optional<Result> find(Path pomPath, Model model) {
        var parent = model.getParent();
        if (parent == null) {
            return Optional.empty();
        }

        return find(pomPath, parent);
    }

    Optional<Result> find(Path pomPath, Parent parent) {
        var filename = "pom.xml";
        var parentRelativePath = parent.getRelativePath();
        var parentPomPath = pomPath.toAbsolutePath().getParent().resolve(parentRelativePath);
        if (!parentPomPath.getFileName().toString().equals(filename)) {
            parentPomPath = parentPomPath.resolve(filename);
        }

        LOG.fine("parentPomPath = " + parentPomPath);
        // If the parent pom doesn't exists, tread the parent as remote parent.
        if (!Files.exists(parentPomPath)) {
            LOG.fine("Resolving the parent pom since the relative path does not exists");
            var result = new Result(ResolutionProvider.getInstance().readModel(
                    parent.getGroupId(),
                    parent.getArtifactId(),
                    parent.getVersion()), true);
            return Optional.of(result);
        }

        try (var is = Files.newInputStream(parentPomPath)) {
            return Optional.of(new Result(reader.read(is, null), false));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
