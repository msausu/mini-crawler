package com.axreng.backend;

import com.axreng.backend.keyword.KeywordId;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.axreng.backend.Definitions.REGEXP_ID;
import static com.axreng.backend.Limits.KEYWORD_MAX;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordIdTest {

    @Test
    void pageIdTest() {
        List<String> ids = IntStream.range(0, KEYWORD_MAX).mapToObj(i -> KeywordId.get()).collect(Collectors.toList());
        Set<String> sids = new HashSet<>();
        ids.forEach(id -> {
            assertTrue(id.matches(REGEXP_ID));
            assertFalse(sids.contains(id));
            sids.add(id);
        });
    }
}
