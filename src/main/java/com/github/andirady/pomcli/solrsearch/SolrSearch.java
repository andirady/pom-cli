package com.github.andirady.pomcli.solrsearch;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
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

public class SolrSearch {

    private static final ObjectMapper OM = JsonMapper.builder().addModule(new AfterburnerModule()).build();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public SolrSearchResult search(SolrSearchRequest req) {
        var httpReq = HttpRequest.newBuilder(makeUri(req)).GET()
                .headers("Accept", "application/json", "Accept-Encoding", "gzip").build();
        try {
            var httpResp = httpClient.send(httpReq, this::gzipBodyHandler);
            return httpResp.body().get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private BodySubscriber<java.util.function.Supplier<SolrSearchResult>> gzipBodyHandler(HttpResponse.ResponseInfo respInfo) {
        var sc = respInfo.statusCode();
        if (sc != 200) {
            throw new IllegalStateException("Server returns error: statusCode=" + sc);
        }

        var contEnc = respInfo.headers().firstValue("Content-Encoding");
        if (!contEnc.filter("gzip"::equals).isPresent()) {
            throw new IllegalStateException("Server returns unexpected encoding: " + contEnc.orElse(""));
        }

        var upstream = HttpResponse.BodySubscribers.ofInputStream();
        return BodySubscribers.mapping(upstream, is -> () -> {
            try (
                var stream = is;
                var gis = new GZIPInputStream(stream);
            ) {
                return OM.readValue(gis, SolrSearchResult.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private URI makeUri(SolrSearchRequest req) {
        try {
            var query = Arrays.stream(SolrSearchRequest.class.getRecordComponents()).map(toKeyValue(req))
                    .filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining("&"));
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
