package com.github.andirady.mq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.PicocliException;
import picocli.CommandLine.Spec;

@Command(name = "add")
public class AddDependency implements Runnable {

	@Option(names = { "-f", "--file" }, defaultValue = "pom.xml")
	Path pomPath;

	@Parameters(arity = "1..*", paramLabel = "groupId:artifactId[:version]")
	List<QuerySpec> coords;

	@Spec
	CommandSpec spec;

	@Override
	public void run() {
		var dbf = DocumentBuilderFactory.newInstance();
		Document doc;
		try {
			var db = dbf.newDocumentBuilder();
			try (var is = Files.newInputStream(pomPath)) {
				doc = db.parse(is);
			}

			var depsElem = (Element) doc.getElementsByTagName("dependencies").item(0);
			if (depsElem == null) {
				depsElem = doc.createElement("dependencies");
				var projectElem = doc.getElementsByTagName("project").item(0);
				if (projectElem == null) {
					throw new IllegalArgumentException("Invalid pom file. No <project> element found.");
				}
				projectElem.appendChild(depsElem);
			}

			coords.stream().parallel().map(this::ensureVersion).map(s -> toElement(doc, s))
					.forEachOrdered(depsElem::appendChild);

			var tf = TransformerFactory.newInstance();
			var xlst = getClass().getClassLoader().getResourceAsStream("pom.xlst");
			var transformer = tf.newTransformer(new StreamSource(xlst));
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
		return elem;
	}
}
