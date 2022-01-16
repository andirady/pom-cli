package com.github.andirady.pomcli.solrsearch;

public record SolrSearchRequest(String q, String core, String sort, int start, int rows) {
}
