package com.axreng.backend.server;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.axreng.backend.Definitions.nowMS;
import static com.axreng.backend.Limits.KEYWORD_CACHE_EXPIRE_MS;

public class KeywordIdCache {
    private static final ConcurrentMap<String, String> searchCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> searchCacheExpire = new ConcurrentHashMap<>();

    private KeywordIdCache() {}
    public static Optional<String> get(String id) {
        Optional<String> result = Optional.empty();
        if (searchCache.containsKey(id)) {
            result = Optional.of(searchCache.get(id));
        }
        return result;
    }

    public static void put(String id, String body) {
        searchCache.putIfAbsent(id, body);
        searchCacheExpire.putIfAbsent(id, nowMS() + KEYWORD_CACHE_EXPIRE_MS);
    }

    public static boolean isExpired(String id) {
        if (searchCacheExpire.containsKey(id) && searchCacheExpire.get(id) > nowMS()) {
            searchCache.remove(id);
            searchCacheExpire.remove(id);
            return true;
        }
        return false;
    }

    public static long age(String id) {
        return searchCacheExpire.get(id) - nowMS();
    }
}
