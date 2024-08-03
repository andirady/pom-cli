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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.io.DefaultModelReader;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.TypeConversionException;

@Command(name = "pom", subcommandsRepeatable = true, subcommands = { IdCommand.class, AddCommand.class,
        SearchCommand.class, SetCommand.class, PlugCommand.class, RemoveCommand.class, SetParentCommand.class })
public class Main {

    public static void main(String[] args) {
        var main = new Main();
        if ("true".equals(System.getProperty("debug"))) {
            main.setDebug(true);
        }

        var cli = createCommandLine(main);
        try (var out = new PrintWriter(System.out, true)) {
            cli.setOut(out);

            System.exit(cli.execute(args));
        }
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

    private String profileId;

    @Option(names = { "-P", "--profile" }, scope = ScopeType.INHERIT)
    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public Optional<String> getProfileId() {
        return Optional.ofNullable(profileId);
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

    static QuerySpec stringToQuerySpec(String s) {
        var qs = QuerySpec.of(s);
        if (qs.groupId() == null) {
            throw new TypeConversionException("Invalid format: missing groupId for '" + s + "'");
        }

        return qs;
    }

    static Dependency stringToDependency(String s) {
        var path = Path.of(s);
        if (Files.isRegularFile(path)) {
            var filename = path.getFileName().toString();
            if (filename.endsWith(".jar")) {
                return readCoordFromJarFile(path);
            } else if (filename.endsWith(".xml")) {
                return readCoordFromPomFile(path);
            }
        } else if (Files.isDirectory(path)) {
            return readCoordFromPomFile(path.resolve("pom.xml"));
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

    private static Dependency readCoordFromPomFile(Path path) {
        if (Files.notExists(path)) {
            throw new TypeConversionException("File not found: " + path);
        }

        var pomReader = new DefaultModelReader(null);
        try (var is = Files.newInputStream(path)) {
            var pom = pomReader.read(is, null);
            var d = new Dependency();
            var g = pom.getGroupId();
            var v = pom.getVersion();
            var parent = pom.getParent();
            if (parent != null) {
                if (g == null || g.isBlank()) {
                    g = parent.getGroupId();
                }
                if (v == null || g.isBlank()) {
                    v = parent.getVersion();
                }
            }

            if (g == null || v == null || g.isBlank() || v.isBlank()) {
                throw new TypeConversionException(path + " is an invalid pom.");
            }

            d.setGroupId(g);
            d.setArtifactId(pom.getArtifactId());
            d.setVersion(v);

            return d;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
