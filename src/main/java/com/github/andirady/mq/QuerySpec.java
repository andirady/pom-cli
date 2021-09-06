package com.github.andirady.mq;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class QuerySpec {

    static QuerySpec of(String spec) {
        var parts = spec.split(":");
        var inst = new QuerySpec();
        switch (parts.length) {
            case 1:
                inst.artifactId(parts[0]);
                break;
            case 2:
                inst.groupId(parts[0]);
                inst.artifactId(parts[1]);
                break;
            case 3:
                inst.groupId(parts[0]);
                inst.artifactId(parts[1]);
                inst.version(parts[2]);
                break;
            default:
                throw new IllegalArgumentException("Invalid spec: " + spec);
        }

        return inst;
    }

    private String groupId;
    private String artifactId;
    private String version;

    private QuerySpec() {
        // NOP
    }

    public String groupId() {
        return groupId;
    }

    public void groupId(String g) {
        groupId = g;
    }

    public String artifactId() {
        return artifactId;
    }

    public void artifactId(String a) {
        artifactId = a;
    }

    public String version() {
        return version;
    }

    public void version(String v) {
        version = v;
    }

    public URI toURI() {
        try {
            return new URI("https", "search.maven.org", "/solrsearch/select", "q=" + toString() + "&start=0&rows=1",
                    null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        var parts = new ArrayList<String>();
        if (groupId != null) {
            parts.add("g:" + groupId);
        }

        if (artifactId != null) {
            parts.add("a:" + artifactId);
        }

        if (version != null) {
            parts.add("v:" + version);
        }

        return String.join(" AND ", parts);
    }
}