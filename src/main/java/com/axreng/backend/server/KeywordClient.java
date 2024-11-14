package com.axreng.backend.server;

import com.axreng.backend.keyword.KeywordResults;
import com.axreng.backend.keyword.KeywordSearch;
import com.google.gson.Gson;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.axreng.backend.Definitions.*;
import static com.axreng.backend.Limits.*;

public class KeywordClient {

    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(3);
    private static final Logger log = LoggerFactory.getLogger(KeywordClient.class);
    private static final URI API;
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(PAGE_TIMEOUT_SEC))
            .build();

    static {
        try {
            API = new URI("http://localhost:" + HTTP_PORT_APPLICATION + KEYWORD_BASE_API);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<BodyId> doSearch(KeywordSearch keyword) throws IOException, InterruptedException {
        return doSearch(keyword, API_REQUEST_INTERVAL_MIN_NS + 1_000);
    }

    public static Optional<BodyId> doSearch(KeywordSearch keyword, long delay) throws IOException, InterruptedException {
        if (delay > 0) { TimeUnit.NANOSECONDS.sleep(delay); }
        final long POLL_INTERVAL = API_REQUEST_INTERVAL_MIN_NS + 1_000;
        HttpRequest request = HttpRequest.newBuilder().uri(API)
                .header(HTTP_HEADER_BODY, new Gson().toJson(keyword, KeywordSearch.class))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int retries = 0;
        while (response.statusCode() == HttpStatus.TOO_MANY_REQUESTS_429 || response.statusCode() == HttpStatus.ENHANCE_YOUR_CALM_420) {
            if (retries++ > 1_00) {
                log.debug("keyword {}, doSearch retries {} exceeded 100", keyword, retries);
                throw new InterruptedException("keyword " + keyword + ", doSearch could obtain response after " + POLL_INTERVAL * retries / 1_000 + " ms");
            }
            TimeUnit.NANOSECONDS.sleep(POLL_INTERVAL);
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        if (response.statusCode() != HttpStatus.OK_200) {
            log.error("keyword {}, api unexpected status code {}", keyword, response.statusCode());
            throw new IOException("keyword " + keyword + ", doSearch http status code " + response.statusCode() + ", cannot proceed");
        }
        Optional<String> header = response.headers().firstValue(HTTP_HEADER_BODY);
        return header.map(s -> new Gson().fromJson(s, BodyId.class));
    }
    public static Optional<KeywordResults> getResults(BodyId body) throws IOException, InterruptedException, URISyntaxException {
        return getResults(body, API_REQUEST_INTERVAL_MIN_NS + 1_000);
    }
    public static Optional<KeywordResults> getResults(BodyId body, long delay) throws IOException, InterruptedException, URISyntaxException {
        if (delay > 0) { TimeUnit.NANOSECONDS.sleep(delay); }
        if (body == null || body.getId() == null || body.getId().isEmpty()) throw new IllegalArgumentException("getResults: body cannot be null");
        HttpRequest request = HttpRequest.newBuilder().uri(new URI(API + "/" + body.getId())).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.debug("getResults bodyId {}, invalid status code {}", body, response.statusCode());
            return Optional.empty();
        }
        if (response.body().isEmpty()) return Optional.empty();
        return Optional.of(new Gson().fromJson(response.body(), KeywordResults.class));
    }

    public static Optional<KeywordResults> syncKeywordSearch(String keyword) throws IOException, InterruptedException, URISyntaxException {
        return syncKeywordSearch(keyword, API_REQUEST_INTERVAL_MIN_NS + 50_000);
    }

    public static Optional<KeywordResults> syncKeywordSearch(String keyword, long delay) throws IOException, InterruptedException, URISyntaxException {
        if (delay > 0) { TimeUnit.NANOSECONDS.sleep(delay); }
        final long POLL_INTERVAL_MS = 250;
        Optional<BodyId> id = doSearch(new KeywordSearch(keyword));
        if (id.isEmpty()) throw new IllegalStateException("no id response from Keyword API");
        log.debug("keyword search for {}, id {}", keyword, id.get().getId());
        Optional<KeywordResults> results = getResults(id.get());
        int retries = 0;
        while (results.isEmpty()) {
            TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            if (retries++ > 10) throw new IllegalStateException("keywordSearch " + keyword + ":  could not obtain id after " + retries + " retries");
            results = getResults(id.get());
        }
        retries = 0;
        while (!results.get().getStatus().equals(SEARCH_STATUS_DONE)) {
            TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            results = getResults(id.get());
            if (results.isEmpty()) {
                if (retries++ > 10) throw new IllegalStateException("keywordSearch: keyword " + keyword + " could not obtain id after " + (POLL_INTERVAL_MS * retries) + " ms");
                results = getResults(id.get());
            }
            log.debug("keyword {}, working, search - #{} urls found", keyword, results.get().getUrls().length);
        }
        return results;
    }

    public static List<Future<Optional<KeywordResults>>> concurrentKeywordSearch(String ... keywords) throws InterruptedException {
        List<Callable<Optional<KeywordResults>>> callables = Arrays.stream(keywords)
                .map(keyword -> (Callable<Optional<KeywordResults>>)() -> syncKeywordSearch(keyword, 6_000))
                .collect(Collectors.toList());
        return SERVICE.invokeAll(callables);
    }

    public static void awaitResults(List<Future<Optional<KeywordResults>>> futures) throws InterruptedException {
        final long START = nowMS();
        final long TIMEOUT_MS = 5 * 60 * PAGE_VISITS_PER_KEYWORD_MAX;
        final long POLL_DELAY_MS = 50;
        Map<Integer, Boolean> isDone = new HashMap<>();
        futures.forEach(future -> isDone.put(future.hashCode(), false));
        for (Future<Optional<KeywordResults>> future : futures) {
            TimeUnit.MILLISECONDS.sleep(POLL_DELAY_MS);
            if (nowMS() - START > TIMEOUT_MS) throw new InterruptedException("awaitResults Timeout");
            isDone.put(future.hashCode(), future.isDone());
            synchronized (isDone) {
                if (isDone.values().stream().allMatch(v -> v)) break;
            }
        }
    }
}
