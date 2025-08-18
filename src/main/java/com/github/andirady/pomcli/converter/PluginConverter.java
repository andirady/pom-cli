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
