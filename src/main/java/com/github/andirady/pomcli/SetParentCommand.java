package com.github.andirady.pomcli;

import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import com.github.andirady.pomcli.converter.ParentConverter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = "parent")
public class SetParentCommand extends ModifyingCommandBase {

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml")
    Path pomPath;

    @Parameters(arity = "1", paramLabel = "PARENT", converter = ParentConverter.class, description = "groupId:artifactId[:version] or path to a directory or pom.xml")
    Parent parent;

    @Option(names = { "-v", "--verbose" })
    boolean verbose;

    @Spec
    CommandSpec spec;

    @Override
    public int process(Model model) throws Exception {
        model.setParent(parent);

        if (verbose) {
            spec.commandLine().getOut().println("Parent is set to " + parent);
        }

        return 0;
    }

    @Override
    Path getPomPath() {
        return pomPath;
    }

}
