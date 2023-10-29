package com.github.andirady.pomcli.converter;

import org.apache.maven.model.Plugin;

import picocli.CommandLine.ITypeConverter;

public class PluginConverter implements ITypeConverter<Plugin> {

    @Override
    public Plugin convert(String value) throws Exception {
        if (value.isBlank()) {
            throw new IllegalArgumentException("value cannot be blank");
        }

        var parts = value.split(":", 3);
        var result = new Plugin();

        String groupId = null;
        String artifactId = null;
        String version = null;

        if (parts.length == 1) {
            artifactId = parts[0];
        } else {
            groupId = parts[0].isBlank() ? null : parts[0];
            artifactId = parts[1];
            if (parts.length > 2) {
                version = parts[2];
            }
        }

        if (groupId != null) {
            result.setGroupId(groupId);
        }

        result.setArtifactId(artifactId);

        if (version != null) {
            result.setVersion(version);
        }

        return result;
    }
}
