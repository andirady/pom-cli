package com.github.andirady.pomcli;

import java.util.logging.*;
import org.apache.maven.model.Dependency;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(name = "pom", subcommandsRepeatable = true, subcommands = {IdCommand.class, AddCommand.class, SearchCommand.class, SetCommand.class})
public class Main {

	public static void main(String[] args) {
		var cli = new CommandLine(new Main());
		cli.registerConverter(QuerySpec.class, Main::stringToQuerySpec);
		cli.registerConverter(Dependency.class, Main::stringToDependency);
		System.exit(cli.execute(args));
	}

    @Option(names = { "-d", "--debug" }, scope = ScopeType.INHERIT)
    public void setDebug(boolean debug) {
        if (!debug) {
            return;
        }

        var rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.FINE);
        var consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        consoleHandler.setLevel(Level.FINE);
        rootLogger.addHandler(consoleHandler);
        rootLogger.fine("OK");
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
