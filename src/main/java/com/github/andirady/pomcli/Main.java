package com.github.andirady.pomcli;

import org.apache.maven.model.Dependency;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "pom", subcommandsRepeatable = true, subcommands = {IdCommand.class, AddCommand.class, SearchCommand.class})
public class Main {

	public static void main(String[] args) {
		var cli = new CommandLine(new Main());
		cli.registerConverter(QuerySpec.class, Main::stringToQuerySpec);
		cli.registerConverter(Dependency.class, Main::stringToDependency);
		System.exit(cli.execute(args));
	}

	private static QuerySpec stringToQuerySpec(String s) {
		var qs = QuerySpec.of(s);
		if (qs.groupId() == null) {
			throw new CommandLine.TypeConversionException("Invalid format: missing groupId for '" + s + "'");
		}

		return qs;
	}

	private static Dependency stringToDependency(String s) {
		var parts = s.split(":");
		if (parts.length < 2) {
			throw new CommandLine.TypeConversionException("Invalid format: missing groupId for '" + s + "'");
		}

		var d = new Dependency();
		d.setGroupId(parts[0]);
		d.setArtifactId(parts[1]);
		if (parts.length >= 3) {
			d.setVersion(parts[2]);
		}
		return d;
	}
}
