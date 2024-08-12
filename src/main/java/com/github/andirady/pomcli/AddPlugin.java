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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;

public class AddPlugin {

    private static final int MAX_RECURSION = 10;
    private final String profileId;

    public AddPlugin() {
        profileId = null;
    }

    public AddPlugin(String profileId) {
        this.profileId = profileId;
    }

    public Plugin addPlugin(Model model, String artifactId) {
        var plugin = new Plugin();
        plugin.setArtifactId(artifactId);
        return addPlugin(model, plugin);
    }

    public Plugin addPlugin(Model model, Plugin plugin) {
        if (plugin.getVersion() == null) {
            if (!resolvePluginManagement(model, plugin, 0)) {
                var query = new QuerySpec(plugin.getGroupId(), plugin.getArtifactId(), null);
                plugin.setVersion(new GetLatestVersion().execute(query).orElseThrow());
            }
        }

        BuildBase build = switch (profileId) {
            case String profileId -> {
                var profile = model.getProfiles().stream().filter(p -> profileId.equals(p.getId())).findFirst()
                        .orElseGet(() -> {
                            var p = new Profile();
                            p.setId(profileId);
                            return p;
                        });

                if (profile.getBuild() instanceof BuildBase b) {
                    yield b;
                }

                var b = new Build();
                profile.setBuild(b);
                model.addProfile(profile);
                yield b;
            }
            case null -> {
                var b = model.getBuild();
                if (b == null) {
                    b = new Build();
                    model.setBuild(b);
                }

                yield b;
            }
        };

        PluginContainer pluginContainer = switch (model.getPackaging()) {
            case "pom" -> {
                if (build.getPluginManagement() == null) {
                    build.setPluginManagement(new PluginManagement());
                }

                yield build.getPluginManagement();
            }
            default -> build;
        };
        var alreadyPlugged = pluginContainer.getPlugins().stream().filter(p -> p.getKey().equals(plugin.getKey()))
                .findFirst().isPresent();
        if (alreadyPlugged) {
            throw new IllegalArgumentException("Plugin %s already plugged".formatted(plugin.getKey()));
        }

        pluginContainer.addPlugin(plugin);

        return plugin;
    }

    private boolean resolvePluginManagement(Model model, Plugin plugin, int loopIndex) {
        if (!(model.getParent() instanceof Parent parent)) {
            return false;
        }

        var parentModel = ResolutionProvider.getInstance().readModel(parent.getGroupId(), parent.getArtifactId(),
                parent.getVersion());
        var managed = Optional.ofNullable(parentModel.getBuild())
                .map(Build::getPluginManagement)
                .filter(Objects::nonNull)
                .map(PluginManagement::getPlugins).map(List::stream)
                .orElseGet(Stream::of)
                .filter(p -> p.getArtifactId().equals(plugin.getArtifactId()))
                .findFirst()
                .orElse(null);

        if (managed != null) {
            plugin.setGroupId(managed.getGroupId());
            return true;
        }

        if (++loopIndex > MAX_RECURSION) {
            throw new IllegalStateException("Exceeded max recursion of " + MAX_RECURSION);
        }

        return resolvePluginManagement(parentModel, plugin, loopIndex);
    }

}
