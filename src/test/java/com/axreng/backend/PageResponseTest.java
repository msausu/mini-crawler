package com.axreng.backend;

import com.axreng.backend.page.PageResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    private static final Logger log = LoggerFactory.getLogger(PageResponse.class);

    @Test
    void pageResponse01OkTest() throws URISyntaxException, IOException, InterruptedException {
        final URI URL_BASE = new URI("http://www.google.com/index.html");
        final String KEYWORD = "google";
        PageResponse page = PageResponse.get(URL_BASE);
        assertEquals(HttpStatus.OK_200, page.getStatusCode());
        boolean ok = page.hasKeyword(KEYWORD);
        List<URI> links = page.getPageLinks(URL_BASE);
        assertTrue(ok);
        assertFalse(links.isEmpty());
        log.info("keyword [{}] search {}", KEYWORD, ok);
        links.forEach(link -> log.info("link {}", link));
    }

    @Test
    void pageResponse01FailTest() throws URISyntaxException, IOException, InterruptedException {
        final URI URL_BASE = new URI("http://www.google.com/index.html");
        final String KEYWORD = "unlikely";
        PageResponse page = PageResponse.get(URL_BASE);
        assertEquals(HttpStatus.OK_200, page.getStatusCode());
        boolean ok = page.hasKeyword(KEYWORD);
        assertFalse(ok);
        log.info("keyword [{}] has keyword {}", KEYWORD, ok);
    }

    @Test
    void pageResponse02okTest() throws URISyntaxException, IOException, InterruptedException {
        final URI URL_BASE = new URI("https://www.wikiwand.com/en/Fermat's_Last_Theorem");
        final String KEYWORD = "fermat";
        PageResponse page = PageResponse.get(URL_BASE);
        assertEquals(HttpStatus.OK_200, page.getStatusCode());
        boolean ok = page.hasKeyword(KEYWORD);
        List<URI> links = page.getPageLinks(URL_BASE);
        assertTrue(ok);
        assertFalse(links.isEmpty());
        log.info("keyword [{}] search {}", KEYWORD, ok);
        links.forEach(link -> log.info("link {}", link));
    }

    @Test
    void pageResponse02FailTest() throws URISyntaxException, IOException, InterruptedException {
        final URI URL_BASE = new URI("https://www.wikiwand.com/en/Fermat's_Last_Theorem");
        final String KEYWORD = "never_never_never";
        PageResponse page = PageResponse.get(URL_BASE);
        assertEquals(HttpStatus.OK_200, page.getStatusCode());
        boolean ok = page.hasKeyword(KEYWORD);
        assertFalse(ok);
        log.info("keyword [{}] has keyword {}", KEYWORD, ok);
    }
}
