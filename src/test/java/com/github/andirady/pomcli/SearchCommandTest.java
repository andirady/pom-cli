package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.github.andirady.pomcli.solrsearch.SolrSearch;
import com.github.andirady.pomcli.solrsearch.SolrSearchResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchCommandTest {

	@Test
	void shouldSortByTimestampIfGroupIdAndArtifactIdIsSpecified() throws Exception {
		try (
			var baos = new ByteArrayOutputStream();
			var solrSearchMocked = mockConstruction(SolrSearch.class)
		) {
			System.setOut(new PrintStream(baos));
			var solrSearch = new SolrSearch();
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
			var result = new SolrSearchResult(resp);
			when(solrSearch.search(any())).thenReturn(result);

			var search = new SearchCommand();
			search.arg = new SearchCommand.Exclusive();
			search.arg.gav = "g:a";
			search.run(solrSearch);

			var expected = """
					Found 4
					g:a:4                                                                                                    \033[32m      a day ago\033[0m
					g:a:3                                                                                                    \033[32m     3 days ago\033[0m
					g:a:2                                                                                                    \033[33m    3 years ago\033[0m
					g:a:1                                                                                                    \033[90m    6 years ago\033[0m
					""";
            var actual = baos.toString();

            assertEquals(expected, actual);
		}
	}
}
