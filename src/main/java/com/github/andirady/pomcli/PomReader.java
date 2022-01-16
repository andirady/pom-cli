package com.github.andirady.pomcli;

import java.io.IOException;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class PomReader {

    public PomReader() {
    }

    public Pom readPom(Path path) throws ParserConfigurationException, SAXException, IOException {
        var dbf = DocumentBuilderFactory.newInstance();
        var db = dbf.newDocumentBuilder();
        var doc = db.parse(path.toFile());
        var project = doc.getElementsByTagName("project").item(0);
        var children = project.getChildNodes();
        var pom = new Pom();
        pom.setPackaging("jar");
        for (var i = 0; i < children.getLength(); i++) {
            var child = children.item(i);
            switch (child.getNodeName()) {
            case "groupId":
                pom.setGroupId(child.getTextContent());
                break;
            case "artifactId":
                pom.setArtifactId(child.getTextContent());
                break;
            case "version":
                pom.setVersion(child.getTextContent());
                break;
            case "packaging":
                pom.setPackaging(child.getTextContent());
                break;
            default:
                break;
            }
        }
        return pom;
    }

}
