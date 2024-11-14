package com.axreng.backend.page;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.axreng.backend.Limits.PAGE_SIZE_MAX;
import static com.axreng.backend.Limits.PAGE_TIMEOUT_SEC;

public class PageResponse {

    private URI url;
    private int statusCode;
    private String page;

    public PageResponse(URI url, int statusCode, String page) {
        if (page == null || url == null) throw new IllegalArgumentException("PageResponse null argument");
        if (statusCode < 200 || statusCode >= 400) throw new IllegalArgumentException("status code [" + statusCode + "] will discard page [" + url.toString() + "]");
        if (page.length() > PAGE_SIZE_MAX) throw new IllegalStateException("HTML page [" + url + "] size greater than " + PAGE_SIZE_MAX);
        this.url = url;
        this.statusCode = statusCode;
        this.page = page;
    }

    public boolean hasKeyword(String keyword) {
        Pattern search = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = search.matcher(page);
        return matcher.find(0);
    }

    public List<URI> getPageLinks(URI base) {
        return PageLinks.sanitize(base, PageLinks.extract(page));
    }

    public static PageResponse get(URI url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(url).build();
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(PAGE_TIMEOUT_SEC))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new PageResponse(url, response.statusCode(), response.body());
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }
}
