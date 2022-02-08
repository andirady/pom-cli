package com.github.andirady.pomcli;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import org.apache.maven.model.Dependency;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "pom",
    subcommandsRepeatable = true,
    subcommands = {IdCommand.class, AddCommand.class, SearchCommand.class, SetCommand.class}
)
public class Main {

	public static void main(String[] args) {
		System.exit(createCommandLine(new Main()).execute(args));
	}

    static CommandLine createCommandLine(Main app) {
		var cli = new CommandLine(app);
        cli.setExecutionExceptionHandler((e, cmd, parseResult) -> {
            var msg = e.getMessage();
            if (msg == null) {
                e.printStackTrace();
            } else {
                cmd.getErr().println(cmd.getColorScheme().errorText(msg));
            }

            return cmd.getExitCodeExceptionMapper() != null
                    ? cmd.getExitCodeExceptionMapper().getExitCode(e)
                    : cmd.getCommandSpec().exitCodeOnExecutionException();
        });
		cli.registerConverter(QuerySpec.class, Main::stringToQuerySpec);
		cli.registerConverter(Dependency.class, Main::stringToDependency);

        return cli;
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
    }

	private static QuerySpec stringToQuerySpec(String s) {
		var qs = QuerySpec.of(s);
		if (qs.groupId() == null) {
			throw new TypeConversionException("Invalid format: missing groupId for '" + s + "'");
		}

		return qs;
	}

	public static Dependency stringToDependency(String s) {
        var path = Path.of(s);
        if (Files.isRegularFile(path)) {
            return readCoordFromJarFile(path);
        }

        var d = new Dependency();
		var parts = s.split(":");
		if (parts.length < 2) {
            d.setArtifactId(parts[0]);
		} else {
            d.setGroupId(parts[0]);
            d.setArtifactId(parts[1]);
            if (parts.length >= 3) {
                d.setVersion(parts[2]);
            }
        }

		return d;
	}
    
    private static Dependency readCoordFromJarFile(Path path) {
        try {
            var uri = URI.create("jar:" + path.toRealPath().toAbsolutePath().toUri());
            try (var fs = FileSystems.newFileSystem(uri, Map.of())) {
                var mavenPath = fs.getPath("", "META-INF", "maven");
                if (Files.notExists(mavenPath)) {
                    throw new TypeConversionException("No maven metadata");
                }

                try (var pathStream = Files.walk(mavenPath)) {
                    var pomPropPath = pathStream.filter(Files::isDirectory)
                            .map(p -> p.resolve("pom.properties"))
                            .filter(Files::exists)
                            .findFirst()
                            .orElseThrow(() -> new TypeConversionException("No maven metadata"));
                    var prop = new Properties();
                    try (var is = Files.newInputStream(pomPropPath)) {
                        prop.load(is);
                    }

                    var d = new Dependency();
                    d.setGroupId(prop.getProperty("groupId"));
                    d.setArtifactId(prop.getProperty("artifactId"));
                    d.setVersion(prop.getProperty("version"));
                    return d;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
