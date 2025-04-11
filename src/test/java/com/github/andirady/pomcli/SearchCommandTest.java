/**
 * Copyright 2021-2024 Andi Rady Kurniawan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

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
import picocli.CommandLine.Help.Ansi;

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

        var expected = Ansi.AUTO.string("""
                Found 4
                g:a:4                                                                                                    @|fg(34)       a day ago|@
                g:a:3                                                                                                    @|fg(34)      3 days ago|@
                g:a:2                                                                                                    @|fg(178)     3 years ago|@
                g:a:1                                                                                                    @|fg(238)     6 years ago|@""");
        var actual = out.toString();

        assertLinesMatch(expected.lines(), actual.lines());
    }
}
