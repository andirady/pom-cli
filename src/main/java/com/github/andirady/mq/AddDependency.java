package com.github.andirady.mq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.PicocliException;
import picocli.CommandLine.Spec;

@Command(name = "add", sortOptions = false)
public class AddDependency implements Runnable {

    private static final Logger LOG = Logger.getLogger("add");

    static class Scope {

        @Option(names = "--compile", description = "Add as compile dependency. This is the default", order = 0)
        boolean compile;

        @Option(names = "--runtime", description = "Add as runtime dependency", order = 1)
        boolean runtime;

        @Option(names = "--provided", description = "Add as provided dependency", order = 2)
        boolean provided;

        @Option(names = "--test", description = "Add as test dependency", order = 3)
        boolean test;

    }

    @Option(names = { "-f", "--file" }, defaultValue = "pom.xml", order = 0)
    Path pomPath;

    @ArgGroup(exclusive = true, multiplicity = "0..1", order = 1)
    Scope scope;

    @Parameters(arity = "1..*", paramLabel = "groupId:artifactId[:version]")
    List<QuerySpec> coords;

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        Document doc;
        Node projectElem;

        try {
            var dbf = DocumentBuilderFactory.newInstance();
            var db = dbf.newDocumentBuilder();
            if (Files.exists(pomPath)) {
                try (var is = Files.newInputStream(pomPath)) {
                    doc = db.parse(is);
                }

                projectElem = doc.getElementsByTagName("project").item(0);
                if (projectElem == null) {
                    throw new IllegalArgumentException("Invalid pom file. No <project> element found.");
                }
            } else {
                LOG.fine(() -> pomPath + " does not exists. Creating a new one");
                doc = db.newDocument();
                projectElem = doc.appendChild(doc.createElement("project"));
                projectElem.appendChild(doc.createElement("modelVersion")).setTextContent("4.0.0");

                var groupId = "unnamed";
                var artifactId = Path.of(System.getProperty("user.dir")).getFileName().toString();
                var version = "0.0.1-SNAPSHOT";

                projectElem.appendChild(doc.createElement("groupId")).setTextContent(groupId);
                projectElem.appendChild(doc.createElement("artifactId")).setTextContent(artifactId);
                projectElem.appendChild(doc.createElement("version")).setTextContent(version);
            }

            var depsElem = (Element) doc.getElementsByTagName("dependencies").item(0);
            if (depsElem == null) {
                depsElem = doc.createElement("dependencies");
                projectElem.appendChild(depsElem);
            }

            coords.stream().parallel().map(this::ensureVersion).map(s -> toElement(doc, s))
                    .forEachOrdered(depsElem::appendChild);

            var tf = TransformerFactory.newInstance();
            Transformer transformer;
            try (var xlst = getClass().getClassLoader().getResourceAsStream("pom.xlst")) {
                if (xlst == null) {
                    throw new IllegalStateException("Fail to read pom.xlst from classpath");
                }
                transformer = tf.newTransformer(new StreamSource(xlst));
            }
            var domSource = new DOMSource(doc);
            try (var os = Files.newOutputStream(pomPath)) {
                var streamResult = new StreamResult(os);
                transformer.transform(domSource, streamResult);
            }
        } catch (PicocliException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    QuerySpec ensureVersion(QuerySpec coord) {
        if (coord.version() == null) {
            var latestVersion = new GetLatestVersion().execute(coord);
            return latestVersion.map(v -> new QuerySpec(coord.groupId(), coord.artifactId(), v))
                    .orElseThrow(() -> new ExecutionException(spec.commandLine(),
                            "No version found: '" + coord.groupId() + ":" + coord.artifactId() + "'"));
        }
        return coord;
    }

    Element toElement(Document doc, QuerySpec spec) {
        var elem = doc.createElement("dependency");
        elem.appendChild(doc.createElement("groupId")).setTextContent(spec.groupId());
        elem.appendChild(doc.createElement("artifactId")).setTextContent(spec.artifactId());
        elem.appendChild(doc.createElement("version")).setTextContent(spec.version());

        if (scope == null) {
            return elem;
        }

        String text = null;
        if (scope.runtime) {
            text = "runtime";
        } else if (scope.provided) {
            text = "provided";
        } else if (scope.test) {
            text = "test";
        }

        if (text != null) {
            elem.appendChild(doc.createElement("scope")).setTextContent(text);
        }

        return elem;
    }
}
