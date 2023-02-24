package com.github.andirady.pomcli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;

public class NewPom {

    private static final Logger LOG = Logger.getLogger(NewPom.class.getName());

    public Model newPom(Path pomPath) {
        var model = new Model();
        model.setModelVersion("4.0.0");
        var parentPom = findParentPom(pomPath, new DefaultModelReader());
        if (parentPom != null) {
            var parent = new Parent();
            parent.setGroupId(parentPom.model().getGroupId());
            parent.setArtifactId(parentPom.model().getArtifactId());
            parent.setVersion(parentPom.model().getVersion());
            var relativePath = pomPath.toAbsolutePath()
                                      .getParent()
                                      .relativize(parentPom.path().getParent()).toString();
            if (!"..".equals(relativePath)) {
                parent.setRelativePath(relativePath);
            }
            model.setParent(parent);
        } else {
            // Only set java version on main poms.
            var props = model.getProperties();
            // Use UTF-8 for default encoding.
            props.setProperty("project.build.sourceEncoding", "UTF-8");

            var majorVersion = getJavaMajorVersion();
            if (Integer.parseInt(majorVersion) < 9) {
                props.setProperty("maven.compiler.source", majorVersion);
                props.setProperty("maven.compiler.target", majorVersion);
            } else {
                props.setProperty("maven.compiler.release", majorVersion);

                new AddPlugin().addPlugin(model, "maven-compiler-plugin");
            }
        }
        return model;
    }

    public String getJavaMajorVersion() {
        try {
            var p = new ProcessBuilder("java", "-version").redirectErrorStream(true).start();
            try (
                var is = p.getInputStream();
                var br = new BufferedReader(new InputStreamReader(is));
            ) {
                return br.lines()
                         .findFirst()
                         .map(s -> s.split("\""))
                         .map(s -> s[1])
                         .map(s -> s.split("\\."))
                         .map(s -> s[0])
                         .orElseThrow();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
