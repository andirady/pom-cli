package com.github.andirady.pomcli.impl;

import com.github.andirady.pomcli.SearchProvider;
import com.github.andirady.pomcli.solrsearch.SolrSearchRequest;
import com.github.andirady.pomcli.solrsearch.SolrSearchResult;

public class WrappingSearchProvider implements SearchProvider {

    private static ThreadLocal<SearchProvider> provider = new ThreadLocal<>();

    public void setProvider(SearchProvider searchProvider) {
        provider.set(searchProvider);
    }

    @Override
    public SolrSearchResult search(SolrSearchRequest req) {
        return provider.get().search(req);
    }
    
}
