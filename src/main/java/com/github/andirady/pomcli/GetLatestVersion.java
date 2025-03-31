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

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

public class GetLatestVersion {

    static final Logger LOG = Logger.getLogger("");
    private static final URI MAVEN_CENTRAL = URI.create("https://repo.maven.apache.org/maven2");

    private final HttpClient client;

    public GetLatestVersion(HttpClient client) {
        this.client = client;
    }

    public GetLatestVersion() {
        this(HttpClient.newBuilder().version(Version.HTTP_2).build());
    }

    public Optional<String> execute(QuerySpec spec) {
        return execute(spec, MAVEN_CENTRAL);
    }

    public Optional<String> execute(QuerySpec spec, URI repository) {
        if (spec.groupId() == null || spec.artifactId() == null) {
            throw new IllegalArgumentException("groupId and artifactId is required");
        }

        try {
            var version = getLatest(repository, spec.groupId(), spec.artifactId(), true);
            return Optional.ofNullable(version);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    URI getMetadataUrl(URI repository, String groupId, String artifactId) throws URISyntaxException {
        return new URI(repository.getScheme(),
                repository.getAuthority(),
                Stream.of(
                        Stream.of(repository.getPath().replaceAll("/$", "")),
                        Arrays.stream(groupId.split("\\.")),
                        Stream.of(artifactId, "maven-metadata.xml"))
                        .flatMap(p -> p)
                        .collect(Collectors.joining("/")),
                null, null);
    }

    String getLatest(URI repository, String groupId, String artifactId, boolean release) throws Exception {
        var uri = getMetadataUrl(repository, groupId, artifactId);
        LOG.fine(() -> "uri = " + uri);
        var t0 = System.currentTimeMillis();
        var request = HttpRequest.newBuilder(uri).GET().build();
        var response = client.send(request, BodyHandlers.ofInputStream());

        LOG.fine(() -> "Responsed in %sms".formatted(System.currentTimeMillis() - t0));

        if (response.statusCode() != 200) {
            LOG.fine(() -> "Status code from " + repository + " is not 200: " + response.statusCode());
            return null;
        }

        var t1 = System.currentTimeMillis();
        var factory = XMLInputFactory.newInstance();
        try (var is = response.body(); var isr = new InputStreamReader(is)) {
            var reader = factory.createXMLStreamReader(isr);
            var inMetadata = false;
            var inVersioning = false;
            var inLatest = false;
            var inRelease = false;
            List<String> versions = null;
            var inVersion = false;

            while (reader.hasNext()) {
                var event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        switch (reader.getLocalName()) {
                            case "metadata":
                                inMetadata = true;
                                break;
                            case "versioning":
                                if (!inMetadata) {
                                    throw new IllegalStateException("Invalid metadata file");
                                }
                                inVersioning = true;
                                break;
                            case "latest":
                                if (!inMetadata && !inVersioning) {
                                    throw new IllegalStateException("Invalid metadata file");
                                }

                                inLatest = true;
                                break;
                            case "release":
                                if (!inMetadata && !inVersioning) {
                                    throw new IllegalStateException("Invalid metadata file");
                                }

                                inRelease = true;
                                break;
                            case "versions":
                                versions = new ArrayList<>();
                                break;
                            case "version":
                                if (versions == null) {
                                    throw new IllegalStateException("Unexpected element <version>");
                                }
                                inVersion = true;
                                break;
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        var text = reader.getText();
                        if (((release && inRelease) || (!release && inLatest))
                                && followsRules(text)) {
                            return text;
                        } else if (inVersion) {
                            versions.add(text);
                        }

                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        switch (reader.getLocalName()) {
                            case "latest":
                                inLatest = false;
                                break;
                            case "release":
                                inRelease = false;
                                break;
                            case "version":
                                inVersion = false;
                                break;
                            case "versions":
                                var version = versions.stream().sorted(Collections.reverseOrder())
                                        .filter(this::followsRules)
                                        .findFirst().orElse(null);
                                // if none follow the rules, return the latest.
                                return (version == null)
                                        ? versions.stream().sorted(Collections.reverseOrder()).limit(1)
                                                .findFirst().orElseThrow()
                                        : version;
                        }
                        break;
                }
            }
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        } finally {
            LOG.fine(() -> "Parsed in %sms".formatted(System.currentTimeMillis() - t1));
        }

        return null;
    }

    private boolean followsRules(String text) {
        return Stream.of("-alpha", "-beta", "-rc").noneMatch(text::contains);
    }

}
