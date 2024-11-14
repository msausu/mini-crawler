package com.axreng.backend.keyword;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.axreng.backend.Limits.KEYWORD_ID_SIZE;

public class KeywordId {

    private static Random random = new Random();

    static {
        random.setSeed(System.nanoTime());
    }

    private KeywordId() {}
    public static String get() {
        byte[] buf = new byte[512];
        random.nextBytes(buf);
        return IntStream.range(0, buf.length).map(i -> buf[i])
                .filter(i -> i > 47 && i < 58 || i > 64 && i < 91 || i > 96 && i < 123)
                .mapToObj(v -> String.valueOf(Character.valueOf((char)v)))
                .limit(KEYWORD_ID_SIZE)
                .collect(Collectors.joining(""));
    }
}
