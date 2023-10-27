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
import picocli.CommandLine.Spec;
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

        while (loop) {
            var req = new SolrSearchRequest(term, core, null, start, 40);
            var resp = solr.search(req).response();
            var docs = resp.docs();

            if (remaining == -1) {
                spec.commandLine().getOut().printf("Found %d%n", resp.numFound());
                remaining = resp.numFound();
            }

            for (var i = 0; i < 2; i++) {
                var stream = docs.stream();
                if (sort) {
                    stream = stream.sorted(Comparator.comparingLong(Document::timestamp).reversed());
                }

                spec.commandLine().getOut().print(
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

                System.console().readLine("\r");
                spec.commandLine().getOut().print("\r");
            }
        }
    }

    private String format(Document doc) {
        var age = new Age(doc.timestamp());
        var ageText = age.toString();
        var years = age.asPeriod().getYears();
        int col = 32;
        if (years > 5) {
            col = 90;
        } else if (years > 2) {
            col = 33;
        }

        return String.format("%-104s \033[" + col + "m%15s\033[0m",
                             doc.id(),
                             ageText);
    }
}
