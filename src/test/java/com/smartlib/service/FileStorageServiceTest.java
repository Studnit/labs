package com.smartlib.service;

import com.smartlib.config.AppProperties;
import com.smartlib.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Security: upload type spoofing and path traversal. */
class FileStorageServiceTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    @TempDir Path dir;

    private FileStorageService svc() { return new FileStorageService(new AppProperties("http://x", dir.toString(), false, "a@b.c", "pw", 5, true)); }

    @Test void realPngAccepted() {
        assertNotNull(svc().save(new MockMultipartFile("f", "a.png", "image/png", PNG), 1L, "feed", FileStorageService.IMAGES));
    }
    @Test void htmlRenamedToPngRejected() {
        var f = new MockMultipartFile("f", "evil.png", "image/png", "<script>alert(1)</script>".getBytes());
        assertThrows(BusinessException.class, () -> svc().save(f, 1L, "feed", FileStorageService.IMAGES));
    }
    @Test void svgAndHtmlExtensionsRejected() {
        for (String n : new String[]{"x.svg", "x.html", "x.php", "x.jsp", "x.exe", "x"})
            assertThrows(BusinessException.class, () -> svc().save(new MockMultipartFile("f", n, "image/png", PNG), 1L, "feed", FileStorageService.IMAGES), n);
    }
    @Test void wrongClientContentTypeRejected() {
        assertThrows(BusinessException.class, () -> svc().save(new MockMultipartFile("f", "a.png", "text/html", PNG), 1L, "feed", FileStorageService.IMAGES));
    }
    @Test void traversalNamesNeverResolve() {
        for (String n : new String[]{"../../etc/passwd", "..%2f..%2fetc%2fpasswd", "a.png", "../1/feed/x.png", "/etc/passwd"})
            assertThrows(ResponseStatusException.class, () -> svc().load(1L, "feed", n), n);
        assertThrows(ResponseStatusException.class, () -> svc().load(1L, "../x", "00000000-0000-0000-0000-000000000000.png"));
    }
    @Test void headerChecks() {
        assertTrue(FileStorageService.headerMatches(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}, "jpg"));
        assertTrue(FileStorageService.headerMatches("GIF89a..".getBytes(), "gif"));
        assertFalse(FileStorageService.headerMatches("GIF89a..".getBytes(), "png"));
        assertFalse(FileStorageService.headerMatches(new byte[]{1, 2}, "png"));
    }
}
