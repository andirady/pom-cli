package com.github.andirady.pomcli;

import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;

import com.github.andirady.pomcli.converter.PluginConverter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "plug")
public class PlugCommand extends ModifyingCommandBase {

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml")
    Path pomPath;

    @Parameters(arity = "1", paramLabel = "groupId:artifactId[:version]", description = "Plugin ID", converter = PluginConverter.class)
    Plugin plugin;

    @Spec
    CommandSpec spec;

    @ParentCommand
    Main main;

    @Override
    public int process(Model model) throws Exception {
        var added = new AddPlugin(main.getProfileId().orElse(null)).addPlugin(model, plugin);
        spec.commandLine().getOut().println("🔌 " + added.getId() + " plugged");

        return 0;
    }

    @Override
    Path getPomPath() {
        return pomPath;
    }

}
