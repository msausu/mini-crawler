package com.axreng.backend;

import com.axreng.backend.server.KeywordAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.axreng.backend.Definitions.KEYWORD_BASE_API;
import static com.axreng.backend.Definitions.URL_BASE;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            String base = args.length == 1 && !args[0].isEmpty() ? args[0] : System.getProperty(URL_BASE);
            new KeywordAPI(base).start(KEYWORD_BASE_API);
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1);
        }
    }
}
