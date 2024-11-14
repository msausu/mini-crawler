package com.axreng.backend.page;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.axreng.backend.Definitions.*;
import static com.axreng.backend.Limits.URI_LENGTH_MAX;

public class PageLinks {

    private PageLinks() {}
    private static final Pattern
            PATTERN_LINK = Pattern.compile(REGEXP_LINK),
            PATTERN_ATTRIBUTE = Pattern.compile(REGEXP_ATTRIBUTE);

    public static List<String> extract(String text) {
        List<String> links = new ArrayList<>();
        Matcher matcher = PATTERN_LINK.matcher(text);
        while (matcher.find()) {
            String link = parseLink(matcher.group(REGEXP_LINK_ATTRIBUTES_GROUP));
            if (!link.isEmpty()) links.add(link);
        }
        return links;
    }

    public static String parseLink(String text) {
        Matcher matcher = PATTERN_ATTRIBUTE.matcher(text);
        while (matcher.find())
            if ("href".compareToIgnoreCase(matcher.group(REGEXP_ATTRIBUTE_NAME_GROUP)) == 0)
                return matcher.group(REGEXP_ATTRIBUTE_VALUE_GROUP).trim();
        return "";
    }

    public static URI sanitize(URI base, String link) {
        if (base.getHost() == null) throw new IllegalArgumentException("missing host in URI " + base);
        try {
            URI url = new URI(link);
            if (url.getHost() != null && url.getHost().compareToIgnoreCase(base.getHost()) != 0) return null;
            URI uri = url.isAbsolute() && !url.isOpaque() ? url : base.resolve(url).normalize();
            String normalizedUrl = uri.toURL().toString();
            if (normalizedUrl.matches(REGEXP_UNNORMALIZED_LINK) || normalizedUrl.length() > URI_LENGTH_MAX) return null;
            return uri;
        } catch (URISyntaxException | MalformedURLException e) {
            return null;
        }
    }
    public static List<URI> sanitize(URI base, List<String> links) {
        return links.stream().map(link -> sanitize(base, link)).filter(Objects::nonNull).collect(Collectors.toList());
    }

}
