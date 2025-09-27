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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;

public class NewPom {

    private static final Logger LOG = Logger.getLogger(NewPom.class.getName());

    public Model newPom(Path pomPath) {
        return newPom(pomPath, false);
    }

    public Model newPom(Path pomPath, boolean standalone) {
        var model = new Model();
        model.setModelVersion("4.0.0");

        if (pomPath.getParent() == null) {
            pomPath = Path.of(System.getProperty("user.dir")).resolve(pomPath);
            // If root, the fileName will remain null, so throw
            if (pomPath.getFileName() == null) {
                throw new IllegalArgumentException("Path not allowed: " + pomPath);
            }
        }

        if (!standalone && findParentPom(pomPath,
                new DefaultModelReader(null)) instanceof ParentPom(Path parentPath, Model parentModel)) {

            var parentGroupId = parentModel.getGroupId();
            var parentVersion = parentModel.getVersion();

            if (parentGroupId == null) {
                parentGroupId = parentModel.getParent() instanceof Parent p ? p.getGroupId() : null;
            }

            if (parentVersion == null) {
                parentVersion = parentModel.getParent() instanceof Parent p ? p.getVersion() : null;
            }

            if (parentGroupId == null || parentVersion == null) {
                throw new IllegalStateException(
                        "Tried to set %s as parent, but it's missing groupId and version".formatted(parentPath));
            }

            var parent = new Parent();
            parent.setGroupId(parentGroupId);
            parent.setArtifactId(parentModel.getArtifactId());
            parent.setVersion(parentVersion);
            var relativePath = pomPath.toAbsolutePath()
                    .getParent()
                    .relativize(parentPath.getParent()).toString();
            if (!"..".equals(relativePath)) {
                parent.setRelativePath(relativePath);
            }
            model.setParent(parent);
        } else {
            // Only set java version on main poms.
            var props = model.getProperties();
            // Use UTF-8 for default encoding.
            props.setProperty("project.build.sourceEncoding", "UTF-8");

            GetJavaMajorVersion.getInstance().get().ifPresent(majorVersion -> {
                if (Double.parseDouble(majorVersion) < 9) {
                    props.setProperty("maven.compiler.source", majorVersion);
                    props.setProperty("maven.compiler.target", majorVersion);
                } else {
                    props.setProperty("maven.compiler.release", majorVersion);

                    new AddPlugin().addPlugin(model, "maven-compiler-plugin");
                }
            });
        }

        model.setVersion("0.0.1-SNAPSHOT");
        model.setArtifactId(pomPath.getParent().getFileName().toString());
        if (model.getParent() == null) {
            model.setGroupId(Config.getInstance().getDefaultGroupId());
            model.setVersion("0.0.1-SNAPSHOT");
        }

        return model;
    }

    private ParentPom findParentPom(Path pomPath, ModelReader pomReader) {
        var parent = pomPath.toAbsolutePath().getParent();
        // To facilitate test:
        if (parent == null) {
            return null;
        }
        // Try to find pom.xml with packaging 'pom' up until 5 ancestor folders.
        for (var i = 0; i < 5; i++) {
            parent = parent.getParent();
            if (parent == null) {
                return null;
            }
            LOG.fine("Looking for pom at " + parent);
            var parentPomPath = parent.resolve("pom.xml");
            if (Files.exists(parentPomPath)) {
                LOG.fine("Found pom.xml at " + parent);
                try (var is = Files.newInputStream(parentPomPath)) {
                    var pom = pomReader.read(is, null);
                    if ("pom".equals(pom.getPackaging())) {
                        LOG.fine(() -> parentPomPath + " is choosen as parent");
                        return new ParentPom(parentPomPath, pom);
                    }
                    LOG.fine(() -> parentPomPath + " is not packaged as pom");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        LOG.fine(() -> "No parent pom found for " + pomPath);
        return null;
    }

    record ParentPom(Path path, Model model) {
    }

}
