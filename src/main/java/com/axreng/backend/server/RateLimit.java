package com.axreng.backend.server;

import java.util.concurrent.atomic.AtomicLong;

import static com.axreng.backend.Definitions.nowNS;
import static com.axreng.backend.Limits.API_REQUEST_INTERVAL_MIN_NS;

public class RateLimit {

    private static final AtomicLong lastSearch = new AtomicLong(System.nanoTime());
    private static final AtomicLong lastResult = new AtomicLong(System.nanoTime());

    private RateLimit() {}
    public enum EndPoint { SEARCH, RESULT }

    public static boolean throttle(EndPoint endPoint) {
        return switch (endPoint) {
            case RESULT -> nowNS() - lastResult.getAndSet(nowNS()) < API_REQUEST_INTERVAL_MIN_NS;
            case SEARCH -> nowNS() - lastSearch.getAndSet(nowNS()) < API_REQUEST_INTERVAL_MIN_NS;
        };
    }
}
