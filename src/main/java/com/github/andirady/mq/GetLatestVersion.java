package com.github.andirady.mq;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GetLatestVersion {

	private static final ObjectMapper OM = new ObjectMapper();

	public Optional<String> execute(QuerySpec spec) {
		if (spec.groupId() == null || spec.artifactId() == null) {
			throw new IllegalArgumentException("groupId and artifactId is required");
		}
		var httpClient = HttpClient.newHttpClient();
		var uri = makeUri(spec);
		var req = HttpRequest.newBuilder(uri).GET().headers("Accept", "application/json", "Accept-Encoding", "gzip")
				.build();
		try {
			var resp = httpClient.send(req, this::gzipBodyHandler);
			return resp.body().response().docs().stream().map(SolrSearchResult.Document::v).findFirst();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return Optional.empty();
	}

	private BodySubscriber<SolrSearchResult> gzipBodyHandler(HttpResponse.ResponseInfo respInfo) {
		var contEnc = respInfo.headers().firstValue("Content-Encoding");
		if (respInfo.statusCode() != 200 || !contEnc.filter("gzip"::equals).isPresent()) {
			throw new IllegalStateException("Server returns error: statusCode=" + respInfo.statusCode());
		}

		return BodySubscribers.mapping(HttpResponse.BodySubscribers.ofInputStream(), is -> {
			try (var gis = new GZIPInputStream(is);
					var isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
			) {
				return OM.readValue(isr, SolrSearchResult.class);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	private URI makeUri(QuerySpec spec) {
		try {
			return new URI("https", "search.maven.org", "/solrsearch/select",
					"q=g:" + spec.groupId() + " AND a:" + spec.artifactId() + "&core=gav&start=0&rows=1", null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}
}
