package com.github.andirady.pomcli;

import picocli.CommandLine.Command;

@Command(name = "props", description = "List properties defined in this POM")
public class PropsCommand extends ReadingOptions implements Runnable {

	@Override
	public void run() {
        var model = getPom().orElseThrow();
        model.getProperties().forEach((k, v) -> System.out.println("%s=%s".formatted(k, v)));
	}
}
