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

import static java.util.stream.Collectors.joining;

import java.util.Comparator;

import com.github.andirady.pomcli.solrsearch.SolrSearchRequest;
import com.github.andirady.pomcli.solrsearch.SolrSearchResult.Document;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = "search")
public class SearchCommand implements Runnable {

    @ArgGroup(exclusive = true, multiplicity = "1")
    Exclusive arg;

    @Spec
    CommandSpec spec;

    static class Exclusive {
        @Parameters(arity = "1")
        String gav;

        @Option(names = { "-c", "--class" })
        String c;

        @Option(names = { "-fc", "--full-class" })
        String fc;
    }

    @Override
    public void run() {
        var solr = SearchProvider.getInstance();
        var core = "";
        var sort = false;
        String term;
        if (arg.c != null) {
            term = "c:" + arg.c;
        } else if (arg.fc != null) {
            term = "fc:" + arg.fc;
        } else {
            var gav = QuerySpec.of(arg.gav);
            term = gav.toString();
            core = "gav";
            sort = gav.groupId() != null;
        }

        int start = 0;
        int remaining = -1;
        var loop = true;
        var out = spec.commandLine().getOut();

        while (loop) {
            var req = new SolrSearchRequest(term, core, null, start, 40);
            var resp = solr.search(req).response();
            var docs = resp.docs();

            if (remaining == -1) {
                out.printf("Found %d%n", resp.numFound());
                remaining = resp.numFound();
            }

            for (var i = 0; i < 2; i++) { // print 20 at a time.
                var stream = docs.stream();
                if (sort) {
                    stream = stream.sorted(Comparator.comparingLong(Document::timestamp).reversed());
                }

                out.printf("%s", 
                        stream.skip(i * 20)
                              .limit(20)
                              .map(d -> format(d))
                              .collect(joining(System.lineSeparator()))
                    );
                
                remaining -= 20;
                start += 20;

                if (remaining <= 0) {
                    System.out.printf("%n");
                    loop = false;
                    break;
                }

                var console = System.console();
                if (console != null) { // console can be null
                    console.readLine("\r");
                }

                out.printf("\r");
            }
        }
    }

    private String format(Document doc) {
        var age = new Age(doc.timestamp());
        var ageText = age.toString();
        var years = age.asPeriod().getYears();
        int col = 34;
        if (years > 5) {
            col = 238;
        } else if (years > 2) {
            col = 178;
        }

        var result = String.format("%-104s @|fg(" + col + ") %15s|@",
                             doc.id(),
                             ageText);
        return Ansi.AUTO.string(result);
    }
}
