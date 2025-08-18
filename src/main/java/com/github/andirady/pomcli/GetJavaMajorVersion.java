/**
 * Copyright 2021-2025 Andi Rady Kurniawan
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
