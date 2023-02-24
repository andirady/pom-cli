package com.github.andirady.pomcli;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;

public class AddPlugin {

    public Plugin addPlugin(Model model, String artifactId) {
        var plugin = new Plugin();
        plugin.setArtifactId(artifactId);
        return addPlugin(model, plugin);
    }

    public Plugin addPlugin(Model model, Plugin plugin) {
        if (plugin.getVersion() == null) {
            var query = new QuerySpec(plugin.getGroupId(), plugin.getArtifactId(), null);
            plugin.setVersion(new GetLatestVersion().execute(query).orElseThrow());
        }

        var build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }

        if ("pom".equals(model.getPackaging())) {
            build.getPluginManagement().addPlugin(plugin);
        } else {
            build.addPlugin(plugin);
        }

        return plugin;
    }

}
