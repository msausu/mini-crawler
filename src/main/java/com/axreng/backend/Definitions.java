package com.axreng.backend;


import org.eclipse.jetty.http.MimeTypes;

import static com.axreng.backend.Limits.*;

public interface Definitions {
    public static final String
            KEYWORD_BASE_API = "/crawl",
            REGEXP_KEYWORD = "[_\\p{IsL}\\d]{" + KEYWORD_SIZE_MIN + "," + KEYWORD_SIZE_MAX + "}",
            REGEXP_ID = "[0-9a-zA-Z]{" + KEYWORD_ID_SIZE + "}",
            REGEXP_LINK = "<[aA]\\s(.*?)>",
            T_SPACE = "\\s*?", T_QUOTE = "['\"]", T_WORD = "\\w*?", T_SOMETHING = ".*?", G_ATTR = "(" + T_WORD + ")", G_VALUE = "(" + T_SOMETHING + ")",
            REGEXP_ATTRIBUTE = T_SPACE + G_ATTR + T_SPACE + "=" + T_SPACE + T_QUOTE + G_VALUE + T_QUOTE,
            REGEXP_UNNORMALIZED_LINK = T_SOMETHING + "\\.\\./.*",
            MEDIA_TYPE_JSON = MimeTypes.Type.APPLICATION_JSON.asString(),
            SEARCH_STATUS_ACTIVE = "active",
            SEARCH_STATUS_DONE = "done",
            URL_BASE = "BASE_URL",
            HTTP_HEADER_BODY = "Body",
            HTTP_HEADER_CACHE = "Cache-Control"
    ;

    public static final int HTTP_PORT_APPLICATION = 4567;
    public static final int REGEXP_LINK_ATTRIBUTES_GROUP = 1;
    public static final int REGEXP_ATTRIBUTE_NAME_GROUP = 1;
    public static final int REGEXP_ATTRIBUTE_VALUE_GROUP = 2;
    public static final boolean ENABLE_CORS = false;
    public static final boolean ENABLE_HEADER_CACHE = false;

    public static long nowMS() { return System.currentTimeMillis(); }
    public static long nowNS() { return System.nanoTime(); }
}
