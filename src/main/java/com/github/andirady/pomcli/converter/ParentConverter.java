package com.github.andirady.pomcli.converter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelReader;

import com.github.andirady.pomcli.GetLatestVersion;
import com.github.andirady.pomcli.QuerySpec;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class ParentConverter implements ITypeConverter<Parent> {

    @Override
    public Parent convert(String spec) {
        var path = Path.of(spec);
        if (Files.exists(path)) {
            path = Files.isDirectory(path) ? path.resolve("pom.xml") : path;

            var pomReader = new DefaultModelReader(null);
            try (var is = Files.newInputStream(path)) {
                var pom = pomReader.read(is, null);
                if (!"pom".equals(pom.getPackaging())) {
                    throw new IllegalArgumentException("The specified parent is not using pom packaging");
                }

                var g = pom.getGroupId();
                var v = pom.getVersion();

                var grandParent = pom.getParent();
                if (grandParent != null) {
                    if (g == null || g.isBlank()) {
                        g = grandParent.getGroupId();
                    }
                    if (v == null || g.isBlank()) {
                        v = grandParent.getVersion();
                    }
                }

                if (g == null || v == null || g.isBlank() || v.isBlank()) {
                    throw new TypeConversionException(path + " is an invalid pom.");
                }

                var parent = new Parent();
                parent.setGroupId(g);
                parent.setArtifactId(pom.getArtifactId());
                parent.setVersion(v);

                return parent;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        var querySpec = QuerySpec.of(spec);
        String version = querySpec.version();
        if (version == null) {
            version = new GetLatestVersion().execute(querySpec)
                    .orElseThrow(() -> new IllegalArgumentException("No version found for " + spec));
        }

        var parent = new Parent();
        parent.setGroupId(querySpec.groupId());
        parent.setArtifactId(querySpec.artifactId());
        parent.setVersion(version);

        return parent;
    }

}
