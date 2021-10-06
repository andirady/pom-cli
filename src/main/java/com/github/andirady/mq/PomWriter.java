package com.github.andirady.mq;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class PomWriter {

    public void write(Pom pom, Path path) throws Exception {
        var dbf = DocumentBuilderFactory.newInstance();
        var db = dbf.newDocumentBuilder();
        Document doc;
        Element projectElem;
        if (Files.isRegularFile(path)) {
            doc = db.parse(path.toFile());

            projectElem = (Element) doc.getElementsByTagName("project").item(0);
            if (projectElem == null) {
                throw new IllegalArgumentException("Invalid pom file. No <project> element found.");
            }
        } else {
            doc = db.newDocument();
            projectElem = (Element) doc.appendChild(doc.createElement("project"));
            projectElem.appendChild(doc.createElement("modelVersion")).setTextContent("4.0.0");
        }

        Node groupIdElem = null;
        Node artifactIdElem = null;
        Node versionElem = null;
        Node packagingElem = null;

        var projectElemChildren = projectElem.getChildNodes();
        for (int i = 0; i < projectElemChildren.getLength(); i++) {
            var child = projectElemChildren.item(i);
            switch (child.getNodeName()) {
            case "groupId":
                groupIdElem = child;
                break;
            case "artifactId":
                artifactIdElem = child;
                break;
            case "version":
                versionElem = child;
                break;
            case "packaging":
                packagingElem = child;
                break;
            }
        }

        setValue(doc, projectElem, groupIdElem, "groupId", pom.getGroupId());
        setValue(doc, projectElem, artifactIdElem, "artifactId", pom.getArtifactId());
        setValue(doc, projectElem, versionElem, "version", pom.getVersion());

        if (!"jar".equals(pom.getPackaging()) || (packagingElem != null && !"jar".equals(packagingElem.getTextContent()))) {
            setValue(doc, projectElem, packagingElem, "packaging", pom.getPackaging());
        }

        var tf = TransformerFactory.newInstance();
        Transformer transformer;
        try (var xlst = getClass().getClassLoader().getResourceAsStream("pom.xlst")) {
            if (xlst == null) {
                throw new IllegalStateException("Fail to read pom.xlst from classpath");
            }
            transformer = tf.newTransformer(new StreamSource(xlst));
        }
        var domSource = new DOMSource(doc);
        try (var os = Files.newOutputStream(path)) {
            var streamResult = new StreamResult(os);
            transformer.transform(domSource, streamResult);
        }
    }

    private void setValue(Document doc, Element parent, Node elem, String nodeName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (elem == null) {
            elem = parent.appendChild(doc.createElement(nodeName));
        }
        elem.setTextContent(value);
    }
}
