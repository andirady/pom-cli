package com.github.andirady.pomcli;

import static java.util.stream.Collectors.toMap;

import java.util.StringJoiner;

import com.github.andirady.pomcli.solrsearch.SolrSearch;
import com.github.andirady.pomcli.solrsearch.SolrSearchRequest;
import com.github.andirady.pomcli.solrsearch.SolrSearchResult.Document;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "search")
public class SearchCommand implements Runnable {

    @ArgGroup(exclusive = true, multiplicity = "1")
    Exclusive arg;

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
        var solr = new SolrSearch();
        var core = "";
        String term;
        if (arg.c != null) {
            term = "c:" + arg.c;
        } else if (arg.fc != null) {
            term = "fc:" + arg.fc;
        } else {
            term = QuerySpec.of(arg.gav).toString();
            core = "gav";
        }

        int start = 0;
        int remaining = -1;

        while (true) {
            var req = new SolrSearchRequest(term, core, null, start, 20);
            var resp = solr.search(req).response();
            var map = resp.docs().stream().collect(toMap(Document::id, Document::timestamp));
            var width = map.keySet().stream().mapToInt(String::length).max().orElse(0);
            var format = "%-" + width + "s %15s";

            if (remaining == -1) {
                System.out.printf("Found %d%n", resp.numFound());
                remaining = resp.numFound();
            }

            var result = new StringJoiner(System.lineSeparator());
            for (var e : map.entrySet()) {
                result.add(String.format(format, e.getKey(), new Age(e.getValue())));
            }

            System.out.printf(result.toString());
            
            remaining -= 20;
            start += 20;

            if (remaining <= 0) {
                System.out.printf("%n");
                break;
            }

            System.console().readLine("\r");
            System.out.printf("\r");
        }
    }
}
