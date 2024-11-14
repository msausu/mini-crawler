package com.axreng.backend;

import com.axreng.backend.keyword.KeywordId;
import com.axreng.backend.keyword.KeywordResults;
import com.axreng.backend.server.BodyId;
import com.axreng.backend.server.KeywordAPI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.axreng.backend.Definitions.*;
import static com.axreng.backend.Limits.*;
import static com.axreng.backend.Limits.PAGE_VISITS_PER_KEYWORD_MAX;
import static com.axreng.backend.server.KeywordClient.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordAPITest {

    private static final Logger log = LoggerFactory.getLogger(KeywordAPITest.class);

    @BeforeAll
    static void setup() throws InterruptedException {
        System.setProperty(URL_BASE, "https://www.wikiwand.com/en/Fermat's_Last_Theorem");
        new KeywordAPI(System.getProperty(URL_BASE), PAGE_VISITS_PER_KEYWORD_MAX / 200).start(KEYWORD_BASE_API);
        TimeUnit.MILLISECONDS.sleep(4_000); // wait some seconds for booting the Jetty server before tests
    }

    @Test
    void absentIdTest() throws InterruptedException, IOException, URISyntaxException {
        assertTrue(getResults(new BodyId(KeywordId.get())).isEmpty());
    }

    @Test
    void apiTest() throws InterruptedException, IOException, URISyntaxException {
        final String KEYWORD = "prime";;
        final long DELAY_NS = API_REQUEST_INTERVAL_MIN_NS + 1_00;
        Optional<KeywordResults> keywordResults1 = syncKeywordSearch(KEYWORD);
        assertTrue(keywordResults1.isPresent());
        assertEquals(SEARCH_STATUS_DONE, keywordResults1.get().getStatus());
        assertTrue(keywordResults1.get().getUrls().length > 0);
        final long START = nowMS();
        Optional<KeywordResults> keywordResults2 = syncKeywordSearch(KEYWORD, DELAY_NS);
        assertEquals(SEARCH_STATUS_DONE, keywordResults2.get().getStatus());
        assertTrue(nowMS() - START < 500);
        assertEquals(keywordResults1, keywordResults2);
    }

    @Test
    void concurrentApiTest() throws InterruptedException, ExecutionException {
        List<Future<Optional<KeywordResults>>> okFutures = concurrentKeywordSearch("number", "theorem", "fermat");
        awaitResults(okFutures);
        for (Future<Optional<KeywordResults>> results : okFutures) {
            assertTrue(results.get().isPresent());
            KeywordResults result = results.get().get();
            assertEquals(SEARCH_STATUS_DONE, result.getStatus());
            assertTrue(result.getUrls().length > 0);
            log.info("concurrentApiTest search id [{}], #{} urls", result.getId(), result.getUrls().length);
        }
    }
}
