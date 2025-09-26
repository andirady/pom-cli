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
package com.github.andirady.pomcli.solrsearch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.github.andirady.pomcli.SearchProvider;

public class SolrSearchProvider implements SearchProvider {

    private static final ObjectMapper OM = JsonMapper.builder().addModule(new AfterburnerModule()).build();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public SolrSearchResult search(SolrSearchRequest req) {
        var httpReq = HttpRequest.newBuilder(makeUri(req)).GET()
                .headers("Accept", "application/json", "Accept-Encoding", "gzip").build();
        try {
            var httpResp = httpClient.send(httpReq, this::bodyHandler);
            return httpResp.body();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private BodySubscriber<SolrSearchResult> bodyHandler(HttpResponse.ResponseInfo respInfo) {
        var sc = respInfo.statusCode();
        if (sc != 200) {
            throw new IllegalStateException("Server returns error: statusCode=" + sc);
        }

        var contEnc = respInfo.headers().firstValue("Content-Encoding");
        var upstream = HttpResponse.BodySubscribers.ofInputStream();

        return BodySubscribers.mapping(upstream, contEnc.filter("gzip"::equals).isPresent()
                ? is -> {
                    try (var gis = new GZIPInputStream(is)) {
                        return OM.readValue(gis, SolrSearchResult.class);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                : is -> {
                    try (is) {
                        return OM.readValue(is, SolrSearchResult.class);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private URI makeUri(SolrSearchRequest req) {
        var query = Arrays.stream(SolrSearchRequest.class.getRecordComponents()).map(toKeyValue(req))
                .filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining("&"));

        try {
            return new URI("https", "search.maven.org", "/solrsearch/select", query, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private Function<RecordComponent, String> toKeyValue(SolrSearchRequest req) {
        return c -> {
            try {
                var value = c.getAccessor().invoke(req);
                if (value == null) {
                    return null;
                }
                return c.getName() + "=" + value;
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnsupportedOperationException(e);
            }
        };
    }
}
