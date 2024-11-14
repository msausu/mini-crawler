package com.axreng.backend;

import com.axreng.backend.keyword.KeywordResults;
import com.axreng.backend.page.PageVisitor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.axreng.backend.Definitions.SEARCH_STATUS_ACTIVE;
import static com.axreng.backend.Definitions.nowMS;
import static com.axreng.backend.Limits.PAGE_VISITS_PER_KEYWORD_MAX;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageVisitorTest {

    private static final Logger log = LoggerFactory.getLogger(PageVisitorTest.class);
    private static final PageVisitor visitor = new PageVisitor(Executors.newCachedThreadPool());

    @Test
    void pageVisitorTest() throws URISyntaxException, InterruptedException {
        final URI BASE_URL = new URI("https://www.wikiwand.com/en/Fermat's_Last_Theorem");
        final String KEYWORD = "fermat";
        final long START = nowMS();
        final int PAGE_VISITS_MAX = PAGE_VISITS_PER_KEYWORD_MAX / 100;
        final int REQUEST_INTERVAL_MS = 75;
        String id = visitor.searchRequest(BASE_URL, KEYWORD, PAGE_VISITS_MAX);
        KeywordResults response = visitor.searchResponse(id);
        int errors = 0;
        while (response != null && response.getStatus().equals(SEARCH_STATUS_ACTIVE)) {
            response = visitor.searchResponse(id);
            if (response == null) log.error("keyword {}, invalid response for existing search: {} count", KEYWORD, errors++);
            TimeUnit.MILLISECONDS.sleep(REQUEST_INTERVAL_MS);
        }
        assertTrue(response.getUrls().length > 0);
        log.info("keyword {}, search id {}, status {}, #{} urls, {} max page visits, invalid ids {}, {} ms",
               KEYWORD , response.getId(), response.getStatus(), response.getUrls().length, PAGE_VISITS_MAX, errors, nowMS() - START);
    }

}
