package com.axreng.backend.page;

import com.axreng.backend.keyword.KeywordId;
import com.axreng.backend.keyword.KeywordResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.axreng.backend.Definitions.SEARCH_STATUS_ACTIVE;
import static com.axreng.backend.Definitions.SEARCH_STATUS_DONE;
import static com.axreng.backend.Limits.*;

public class PageVisitor {

    private static final Logger log = LoggerFactory.getLogger("visit");
    private final Object monitor = new Object();
    private final ExecutorService pageExecutor;
    private final ExecutorService apiExecutor = Executors.newFixedThreadPool(PAGE_VISITOR_THREADS_MAX);
    private enum Status { ACTIVE, DONE, EXPIRED }
    private final ConcurrentMap<String, Set<URI>> idUrls = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Status> idStatus = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> idKeyword = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> keywordId = new ConcurrentHashMap<>();
    private final int maxPageVisits;

    public PageVisitor(ExecutorService pageExecutor) {
        this(pageExecutor, PAGE_VISITS_PER_KEYWORD_MAX);
    }

    public PageVisitor(ExecutorService pageExecutor, int maxPageVisits) {
        this.pageExecutor = pageExecutor;
        this.maxPageVisits = maxPageVisits;
    }
    private void search(final URI base, final String id, final String keyword, final int maxPageVisits) {
        final AtomicInteger visitCount = new AtomicInteger(1);
        final AtomicInteger workCount = new AtomicInteger(0);
        final BlockingQueue<URI> unvisited = new LinkedBlockingQueue<>();
        final Set<URI> visited = new ConcurrentSkipListSet<>();
        int retries = 0;
        unvisited.add(base);
        while (!unvisited.isEmpty() || workCount.get() > 0 || retries < URI_QUEUE_RETRIES_MAX) {
            log.trace("keyword {}, queue size {}, workers {}, urls {}", keyword, unvisited.size(), workCount.get(), idUrls.get(id).size());
            try {
                URI next = unvisited.poll(URI_QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (next == null) {
                    retries++;
                    continue;
                } else {
                    retries = retries > 0 ? retries - 1 : retries;
                }
                log.trace("keyword {}, next uri {}", keyword, next);
                if (visited.contains(next)) {
                    log.trace("keyword {}, skip visited uri {}", keyword, next);
                    continue;
                }
                if (workCount.get() > KEYWORD_WORKER_CONCURRENCY_MAX || unvisited.size() > URI_QUEUE_SIZE_MAX) continue;
                workCount.incrementAndGet();
                visit(next, id, keyword, maxPageVisits, unvisited, visited, visitCount, workCount);
            } catch (InterruptedException e) {
                log.warn(e.getMessage());
            }
        }
        idStatus.put(id, Status.DONE);
        log.trace("keyword {}, done", keyword);
    }

    private void visit(URI next, String id, String keyword, int maxPageVisits, BlockingQueue<URI> unvisited, Set<URI> visited, AtomicInteger visitCount, AtomicInteger workCount) {
        pageExecutor.submit(() -> {
            try {
                if (visitCount.get() > maxPageVisits) {
                    log.warn("keyword {}, max page visit count {} reached", keyword, maxPageVisits);
                    return;
                }
                long start = System.currentTimeMillis();
                int linkCount = -1;
                PageResponse page = PageResponse.get(next);
                if (page.hasKeyword(keyword)) {
                    idUrls.merge(id, newUrlsSet(next), (x, y) -> { x.add(next); return x; });
                    linkCount = queueUnvisitedUrls(keyword, unvisited, page.getPageLinks(next));
                }
                visitCount.incrementAndGet();
                log.trace("keyword {}, page {}, {}, visited in {} ms", keyword, next, (linkCount < 0 ? "keyword absent" : linkCount + " links"), System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.trace("keyword {} search error: {}", keyword, e.getMessage());
            } finally {
                visited.add(next);
                workCount.decrementAndGet();
            }
        });
    }

    private int queueUnvisitedUrls(String keyword, BlockingQueue<URI> unvisited, List<URI> links) {
        for (URI link : links)
            try {
                unvisited.put(link);
            } catch (InterruptedException e) {
                log.warn("keyword {}, could not add link {} to queue", keyword, link);
            }
        return links.size();
    }

    private Set<URI> newUrlsSet(URI initial) {
        Set<URI> s = new ConcurrentSkipListSet<>();
        s.add(initial);
        return s;
    }

    private String newKeywordId() {
        String id = KeywordId.get();
        int retries = 0;
        while (idStatus.containsKey(id) && retries++ < KEYWORD_ID_RETRIES_MAX ) {
            id = KeywordId.get();
        }
        if (retries == 10) throw new IllegalStateException("failed to gererate new unique KeywordId");
        return id;
    }

    public void refresh(String id) {
        idStatus.put(id, Status.EXPIRED);
    }

    public void validateOperationalLimits() {
        if (idStatus.values().stream().filter(status -> status == Status.ACTIVE).count() > KEYWORD_ACTIVE_MAX) throw new IllegalStateException("too many simultaneous pending search requests");
        if (idStatus.size() > KEYWORD_MAX) throw new IllegalStateException("keyword limit reached");
    }

    public String startSearch(URI base, String keyword, String id, int maxPageVisits) {
        synchronized (monitor) {
            final String existingId = keywordId.putIfAbsent(keyword, id);
            if (existingId != null && !existingId.isEmpty()) return existingId;
            idKeyword.putIfAbsent(id, keyword);
            idUrls.putIfAbsent(id, new ConcurrentSkipListSet<>());
            idStatus.putIfAbsent(id, Status.ACTIVE);
            try {
                return id;
            } finally {
                apiExecutor.submit(() -> search(base, id, keyword, maxPageVisits));
                log.debug("keyword {}, started started with id [{}]", keyword, id);
            }
        }
    }

    public String searchRequest(URI base, String keyword) {
        return searchRequest(base, keyword, this.maxPageVisits);
    }

    public String searchRequest(URI base, String keyword, int maxPageVisits) {
        if (keywordId.containsKey(keyword)) {
            String id = keywordId.get(keyword);
            log.debug("keyword {}, found id {}", keyword, id);
            if (idStatus.get(id) != Status.EXPIRED) {
                return id;
            } else { // we may presume no active workers for this id
                idUrls.merge(id, new ConcurrentSkipListSet<>(), (x, y) -> new ConcurrentSkipListSet<>());
                idStatus.put(id, Status.ACTIVE);
                try {
                    return id;
                } finally {
                    apiExecutor.submit(() -> search(base, id, keyword, maxPageVisits));
                }
            }
        } else {
            validateOperationalLimits();
            return startSearch(base, keyword, newKeywordId(), maxPageVisits);
        }
    }

    public KeywordResults searchResponse(String id) {
        if (!idStatus.containsKey(id)) return null;
        log.debug("search response for {}", id);
        return new KeywordResults(
                id,
                switch (idStatus.get(id)) {
                    case DONE, EXPIRED -> SEARCH_STATUS_DONE;
                    case ACTIVE -> SEARCH_STATUS_ACTIVE;
                },
                idUrls.get(id).toArray(URI[]::new)
        );
    }
}
