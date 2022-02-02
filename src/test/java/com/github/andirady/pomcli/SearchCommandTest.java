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
					new SolrSearchResult.Document("g:a:1", "g", "a", "1", null,
							now.minus(Duration.ofDays(3)).toEpochMilli()), // oldest
					new SolrSearchResult.Document("g:a:2", "g", "a", "2", null,
							now.minus(Duration.ofDays(1)).toEpochMilli()) // newest
				);
			var resp = new SolrSearchResult.Response(docs.size(), 0, docs);
			var result = new SolrSearchResult(resp);
			when(solrSearch.search(any())).thenReturn(result);

			var search = new SearchCommand();
			search.arg = new SearchCommand.Exclusive();
			search.arg.gav = "g:a";
			search.run(solrSearch);

			assertEquals("""
					Found 2
					g:a:2       a day ago
					g:a:1      3 days ago
					""".replaceAll("\n", System.lineSeparator()), baos.toString());
		}
	}
}
