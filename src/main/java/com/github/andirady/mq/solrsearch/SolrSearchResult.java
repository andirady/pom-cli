package com.github.andirady.mq.solrsearch;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolrSearchResult(Response response) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static record Response(int numFound, int start, List<Document> docs) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static record Document(String id, String g, String a, String v, String latestVersion, long timestamp) {
	}
}
