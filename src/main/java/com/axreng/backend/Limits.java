package com.axreng.backend;

// These limits should be set according to HW capacity
public interface Limits {
    public static long API_REQUEST_INTERVAL_MIN_NS = 5_000;
    public static int PAGE_VISITOR_THREADS_MAX = 4;
    public static int KEYWORD_WORKER_CONCURRENCY_MAX = 50;
    public static int KEYWORD_ID_SIZE = 8;
    public static int KEYWORD_ID_RETRIES_MAX = 3;
    public static int KEYWORD_SIZE_MIN = 4;
    public static int KEYWORD_SIZE_MAX = 32;
    public static int KEYWORD_MAX = 1_000_000;
    public static int KEYWORD_ACTIVE_MAX = 1_00; // max simultaneous keyword searches in progress
    public  static long KEYWORD_CACHE_EXPIRE_SEC = 24 * 60 * 60L;
    public  static long KEYWORD_CACHE_EXPIRE_MS = KEYWORD_CACHE_EXPIRE_SEC * 1_000;
    public  static int PAGE_SIZE_MAX = 32_000_000;
    public static int PAGE_TIMEOUT_SEC = 15;
    public static int PAGE_VISITS_PER_KEYWORD_MAX = 1_000; // max pages that will be analyzed for a keyword search
    public static int URI_LENGTH_MAX = 2048;
    public static int URI_QUEUE_TIMEOUT_MS = 5;
    public static int URI_QUEUE_RETRIES_MAX = 25;
    public static int URI_QUEUE_SIZE_MAX = KEYWORD_WORKER_CONCURRENCY_MAX * KEYWORD_ACTIVE_MAX;

}
