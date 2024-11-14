package com.axreng.backend.keyword;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

public class KeywordResults {
    private final String id;
    private final String status;
    private final String[] urls;

    public KeywordResults(String id, String status, URI[] urls) {
        this.id = id;
        this.status = status;
        this.urls = Arrays.stream(urls).map(URI::toString).toArray(String[]::new);
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String[] getUrls() {
        return urls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeywordResults results = (KeywordResults) o;
        return Objects.equals(id, results.id) && Objects.equals(status, results.status) && Arrays.equals(urls, results.urls);
    }
}
