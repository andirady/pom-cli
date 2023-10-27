package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.andirady.pomcli.impl.WrappingSearchProvider;
import com.github.andirady.pomcli.solrsearch.SolrSearchRequest;
import com.github.andirady.pomcli.solrsearch.SolrSearchResult;

import picocli.CommandLine;

class SearchCommandTest {

    private CommandLine underTest;

    @BeforeEach
    void setup() {
        var app = new Main();
        underTest = Main.createCommandLine(app);
    }

	@Test
	void sortByTimestampWhenGroupIdAndArtifactIdSpecified() throws Exception {
        if (SearchProvider.getInstance() instanceof WrappingSearchProvider sp) {
            sp.setProvider(new SearchProvider() {
                @Override
                public SolrSearchResult search(SolrSearchRequest req) {
                    var now = Instant.now();
                    var docs = List.of(
                            new SolrSearchResult.Document("g:a:2", "g", "a", "2", null,
                                    now.minus(Duration.ofDays(3 * 365 + 3)).toEpochMilli()), // 3 years old
                            new SolrSearchResult.Document("g:a:1", "g", "a", "1", null,
                                    now.minus(Duration.ofDays(6 * 365 + 6)).toEpochMilli()), // ~6 years old
                            new SolrSearchResult.Document("g:a:3", "g", "a", "3", null,
                                    now.minus(Duration.ofDays(3)).toEpochMilli()), // < 3 years old
                            new SolrSearchResult.Document("g:a:4", "g", "a", "4", null,
                                    now.minus(Duration.ofDays(1)).toEpochMilli()) // latest
                        );
                    var resp = new SolrSearchResult.Response(docs.size(), 0, docs);
                    return new SolrSearchResult(resp);
                }
            });
        }

        var out = new StringWriter();
        underTest.setOut(new PrintWriter(out));

        underTest.execute("search", "g:a");

        var expected = """
                Found 4
                g:a:4                                                                                                    \033[32m      a day ago\033[0m
                g:a:3                                                                                                    \033[32m     3 days ago\033[0m
                g:a:2                                                                                                    \033[33m    3 years ago\033[0m
                g:a:1                                                                                                    \033[90m    6 years ago\033[0m""";
        var actual = out.toString();

        assertEquals(expected, actual);
	}
}
