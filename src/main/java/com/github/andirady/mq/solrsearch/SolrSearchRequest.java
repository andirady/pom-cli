package com.github.andirady.mq.solrsearch;

public record SolrSearchRequest(String q, String core, String sort, int start, int rows) {
}
