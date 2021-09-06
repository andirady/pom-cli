package com.github.andirady.mq;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.util.zip.*;

import org.json.*;

public class MavenQuery {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.exit(1);
        }

        try {
            var app = new MavenQuery();
            app.start(args);
            System.exit(0);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public void start(String[] args) throws Exception {
        var hc = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        var reqStream = Arrays.stream(args).map(s -> QuerySpec.of(s).toURI())
                .map(u -> HttpRequest.newBuilder().uri(u).headers("Accept", "application/json, text/plain, */*",
                        "Accept-Encoding", "gzip, deflate, br", "Keep-Alive", "true").build());
        if (args.length > 2) {
            reqStream = reqStream.parallel();
        }

        var results = reqStream.map(req -> {
            try {
                var resp = hc.send(req, this::gzipBodyHandler);
                return new JSONObject(resp.body()).getJSONObject("response");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).filter(o -> o.getInt("numFound") > 0).map(o -> toRemoteURI(o.getJSONArray("docs").getJSONObject(0)))
                .collect(Collectors.toList());
        System.out.println(results);
    }

    private BodySubscriber<String> gzipBodyHandler(HttpResponse.ResponseInfo respInfo) {
        var contEnc = respInfo.headers().firstValue("Content-Encoding");
        if (respInfo.statusCode() != 200 || !contEnc.filter("gzip"::equals).isPresent()) {
            return HttpResponse.BodySubscribers.ofString(Charset.forName("UTF-8"));
        }

        return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofByteArray(), bytes -> {
            try (var gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
                    var br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));) {
                return br.lines().collect(Collectors.joining());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private URI toRemoteURI(JSONObject json) {
        try {
            var g = json.getString("g");
            var a = json.getString("a");
            var v = json.optString("v");
            if (v.isBlank()) {
                v = json.getString("latestVersion");
            }
            var fileName = a + "-" + v + ".jar";
            var path = Arrays.stream(g.split("\\.")).map(Path::of)
                    .collect(Collectors.reducing(Path.of(""), Path::resolve)).resolve(a).resolve(v).resolve(fileName);
            return new URI("https", "search.maven.org", "/remotecontent", "filepath=" + path, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
