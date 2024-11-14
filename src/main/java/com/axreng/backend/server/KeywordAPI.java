package com.axreng.backend.server;

import com.axreng.backend.keyword.KeywordResults;
import com.axreng.backend.keyword.KeywordSearch;
import com.axreng.backend.page.PageVisitor;
import com.google.gson.Gson;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.Executors;

import static com.axreng.backend.Limits.*;
import static com.axreng.backend.server.KeywordIdCache.age;
import static com.axreng.backend.server.RateLimit.*;
import static com.axreng.backend.Definitions.*;
import static com.axreng.backend.server.RateLimit.EndPoint.*;
import static spark.Spark.*;

public class KeywordAPI {

    private static final Logger log = LoggerFactory.getLogger(KeywordAPI.class);
    private final PageVisitor visitor;
    private final URI base;

    public KeywordAPI(String base) {
        this(base, PAGE_VISITS_PER_KEYWORD_MAX);
    }
    public KeywordAPI(String base, int maxVisitsPerKeyword) {
        if (maxVisitsPerKeyword < 1 || maxVisitsPerKeyword > 100_000) throw new IllegalArgumentException("invalid value " + maxVisitsPerKeyword +  " for maxVisits per keyword");
        if (base == null || base.isEmpty()) throw new IllegalArgumentException(URL_BASE + " empty");
        try {
            this.base = new URI(base);
            if (this.base.toURL().toString().length() > URI_LENGTH_MAX
                    || this.base.getHost() == null
                    || this.base.getHost().isEmpty()
                    || this.base.getScheme() == null
                    || !("http".compareToIgnoreCase(this.base.getScheme()) == 0 || "https".compareToIgnoreCase(this.base.getScheme()) == 0))
                throw new MalformedURLException();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException(URL_BASE + " incorrect");
        }
        this.visitor= new PageVisitor(Executors.newCachedThreadPool(), maxVisitsPerKeyword);
    }
    public void start(String baseAPI) {
        port(HTTP_PORT_APPLICATION);
        if (ENABLE_CORS) enableCors();
        serveSearchRequest(baseAPI);
        serveSearchResult(baseAPI);
        log.info("Application started at port {}, {} is {}", HTTP_PORT_APPLICATION, URL_BASE, base);
    }

    private void serveSearchResult(String baseAPI) {
        get(baseAPI + "/:id", (req, res) -> {
            res.type(MEDIA_TYPE_JSON);
            String id = req.params("id");
            if (id == null || !id.matches(REGEXP_ID)) {
                res.status(HttpStatus.BAD_REQUEST_400);
                return "";
            }
            if (KeywordIdCache.isExpired(id)) {
                visitor.refresh(id);
            } else {
                Optional<String> result = KeywordIdCache.get(id);
                if (result.isPresent()) {
                    res.status(HttpStatus.OK_200);
                    if (ENABLE_HEADER_CACHE) res.header(HTTP_HEADER_CACHE, "max-age=" + age(id));
                    return result.get();
                }
            }
            if (throttle(RESULT)) {
                res.status(HttpStatus.TOO_MANY_REQUESTS_429);
                return "";
            }
            try {
                KeywordResults response = visitor.searchResponse(id);
                if (response != null) {
                    String body = new Gson().toJson(response);
                    if (response.getStatus().equals(SEARCH_STATUS_DONE)) {
                        KeywordIdCache.put(id, body);
                        if (ENABLE_HEADER_CACHE) res.header(HTTP_HEADER_CACHE, "max-age=" + KEYWORD_CACHE_EXPIRE_SEC);
                    } else {
                        if (ENABLE_HEADER_CACHE) res.header(HTTP_HEADER_CACHE, "max-age=0");
                    }
                    res.status(HttpStatus.OK_200);
                    return body;
                } else {
                    log.warn("Unknown search id [{}] request", id);
                    res.status(HttpStatus.NOT_FOUND_404);
                    return "";
                }
            } catch (IllegalStateException e) {
                log.error(e.getMessage());
                res.status(HttpStatus.TOO_MANY_REQUESTS_429);
                return "";
            }
        });
    }

    private void serveSearchRequest(String baseAPI) {
        post(baseAPI, (req, res) -> {
            res.type(MEDIA_TYPE_JSON);
            if (throttle(SEARCH)) {
                res.status(HttpStatus.TOO_MANY_REQUESTS_429);
                return "";
            }
            KeywordSearch request = new Gson().fromJson(req.headers(HTTP_HEADER_BODY), KeywordSearch.class);
            String keyword = request.getKeyword();
            if (keyword == null || !keyword.matches(REGEXP_KEYWORD)) {
                log.error("Invalid keyword [{}] request", keyword);
                res.status(HttpStatus.BAD_REQUEST_400);
                return "";
            }
            try {
                String id = visitor.searchRequest(base, keyword);
                if (id == null) {
                    res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    return "";
                } else {
                    res.status(HttpStatus.OK_200);
                    res.header(HTTP_HEADER_BODY, new Gson().toJson(new BodyId(id), BodyId.class));
                    return "";
                }
            } catch (IllegalStateException e) {
                log.warn(e.getMessage());
                res.status(HttpStatus.ENHANCE_YOUR_CALM_420);
                return "";
            }
        });
    }
    private void enableCors() {
        options("/*", (req, res) -> {
            String headers = req.headers("Access-Control-Request-Headers");
            if (headers != null) res.header("Access-Control-Allow-Headers", headers);
            String method = req.headers("Access-Control-Request-Method");
            if (method != null) res.header("Access-Control-Allow-Methods", method);
            return "OK";
        });
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Headers", "*");
            res.type(MEDIA_TYPE_JSON);
        });
    }
}
