package com.github.andirady.pomcli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;

public class NewPom {

    public Model newPom(Path pomPath) {
        var model = new Model();
        model.setModelVersion("4.0.0");
        var parentPom = findParentPom(pomPath, new DefaultModelReader());
        if (parentPom != null) {
            var parent = new Parent();
            parent.setGroupId(parentPom.model().getGroupId());
            parent.setArtifactId(parentPom.model().getArtifactId());
            parent.setVersion(parentPom.model().getVersion());
            parent.setRelativePath(pomPath.relativize(parentPom.path().getParent()).toString());
            model.setParent(parent);
        }
        var props = model.getProperties();
        var majorVersion = getJavaMajorVersion();
        props.setProperty("maven.compiler.source", majorVersion);
        props.setProperty("maven.compiler.target", majorVersion);
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
        var parent = pomPath.getParent();
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
            var parentPomPath = parent.resolve("pom.xml");
            if (Files.exists(parentPomPath)) {
                try (var is = Files.newInputStream(parentPomPath)) {
                    var pom = pomReader.read(is, null);
                    if ("pom".equals(pom.getPackaging())) {
                        return new ParentPom(parentPomPath, pom);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return null;
    }

    record ParentPom(Path path, Model model) {
    }

}
