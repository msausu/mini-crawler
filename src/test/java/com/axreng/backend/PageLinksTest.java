package com.axreng.backend;

import com.axreng.backend.page.PageLinks;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class PageLinksTest {

    @Test
    void sanitizeLinkTest() throws URISyntaxException, IOException {
        final String BASE = "http://x.y.z";
        final URI URL_BASE = new URI(BASE + "/p1/p2/p3/page.html");
        assertNull(PageLinks.sanitize(URL_BASE, "http://www.wikipedia.com"));
        assertNull(PageLinks.sanitize(URL_BASE, "http://a.b.c/d/e"));
        assertNull(PageLinks.sanitize(URL_BASE, BASE + "/a/.../b"));
        assertNull(PageLinks.sanitize(URL_BASE, BASE + "/a/ /b"));
        assertEquals(PageLinks.sanitize(URL_BASE, URL_BASE + "/a/b/c.html").toURL().toString(), URL_BASE + "/a/b/c.html");
        assertEquals(PageLinks.sanitize(URL_BASE, URL_BASE + "/c.html").toURL().toString(), URL_BASE + "/c.html");
        assertEquals(PageLinks.sanitize(URL_BASE, "/a/b/c/").toURL().toString(), BASE + "/a/b/c/");
        assertEquals(PageLinks.sanitize(URL_BASE, "/a/b/c").toURL().toString(), BASE + "/a/b/c");
        assertEquals(PageLinks.sanitize(URL_BASE, "./a/b/c").toURL().toString(), BASE + "/p1/p2/p3/a/b/c");
        assertEquals(PageLinks.sanitize(URL_BASE, "./a/b/c.html").toURL().toString(), BASE + "/p1/p2/p3/a/b/c.html");
        assertEquals(PageLinks.sanitize(URL_BASE, "../a/b/c.html").toURL().toString(), BASE + "/p1/p2/a/b/c.html");
        assertEquals(PageLinks.sanitize(URL_BASE, "../../a/b/c.html").toURL().toString(), BASE + "/p1/a/b/c.html");
        assertEquals(PageLinks.sanitize(URL_BASE, "../../../a/b/c.html").toURL().toString(), BASE + "/a/b/c.html");
        assertNull(PageLinks.sanitize(URL_BASE, "../../../../a/b/c.html"));
    }
}
