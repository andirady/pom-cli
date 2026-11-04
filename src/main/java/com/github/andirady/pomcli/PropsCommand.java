package com.github.andirady.pomcli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "props", description = "List properties defined in this POM")
public class PropsCommand extends ReadingOptions implements Runnable {

    @Spec
    CommandSpec spec;

	@Override
	public void run() {
        var model = getPom().orElseThrow();
        model.getProperties().forEach((k, v) -> spec.commandLine().getOut().println("%s=%s".formatted(k, v)));
	}
}
