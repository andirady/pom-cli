package com.github.andirady.pomcli;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface GetJavaMajorVersion {

    static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("\"(\\d+)(-ea)?(\\.(\\d+))?(\\.(.+))?\"");

    static GetJavaMajorVersion getInstance() {
        return ServiceLoader.load(GetJavaMajorVersion.class).findFirst().orElseThrow(); 
    }

    default String get() {
        try (
            var br = getBufferedReader();
        ) {
            return br.lines()
                     .findFirst()
                     .map(JAVA_VERSION_PATTERN::matcher)
                     .filter(Matcher::find)
                     .map(m -> {
                         var i = m.group(1);
                         return "1".equals(i) ? (i + m.group(3)) : i;
                     })
                     .orElseThrow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    BufferedReader getBufferedReader() throws IOException;
}
