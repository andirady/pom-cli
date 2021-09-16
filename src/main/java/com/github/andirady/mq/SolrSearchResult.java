package com.github.andirady.mq;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolrSearchResult(Response response) {
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static record Response(int numFound, int start, List<Document> docs) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
    public static record Document(String id, String g, String a, String v, String latestVersion) {}
}
