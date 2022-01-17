package com.github.andirady.pomcli;

import static java.util.stream.Collectors.joining;

import java.util.Comparator;

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
        run(solr);
    }

    void run(SolrSearch solr) {
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
        int width = 0;

        while (true) {
            var req = new SolrSearchRequest(term, core, null, start, 20);
            var resp = solr.search(req).response();
            var docs = resp.docs();
            var w = docs.stream().map(Document::id).mapToInt(String::length).max().orElse(0);
            if (w > width) {
                width = w;
            }

            var format = "%-" + width + "s %15s";

            if (remaining == -1) {
                System.out.printf("Found %d%n", resp.numFound());
                remaining = resp.numFound();
            }

            var stream = docs.stream();
            if (sort) {
                stream = stream.sorted(Comparator.comparingLong(Document::timestamp).reversed());
            }
            System.out.println(
                    stream.map(d -> String.format(format, d.id(), new Age(d.timestamp())))
                          .collect(joining(System.lineSeparator()))
                );
            
            remaining -= 20;
            start += 20;

            if (remaining <= 0) {
                System.out.printf("%n");
                break;
            }

            System.console().readLine("\r");
            System.out.print("\r");
        }
    }
}
