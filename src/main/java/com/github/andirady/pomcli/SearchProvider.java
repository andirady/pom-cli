package com.github.andirady.pomcli;

import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import com.github.andirady.pomcli.solrsearch.SolrSearchRequest;
import com.github.andirady.pomcli.solrsearch.SolrSearchResult;

public interface SearchProvider {

    static SearchProvider getInstance() {
        return ServiceLoader.load(SearchProvider.class).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No provider for " + SearchProvider.class));
    }

    SolrSearchResult search(SolrSearchRequest req);

}
