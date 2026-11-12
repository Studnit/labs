package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;

/** Serves feed images only to authenticated users of the same library (path is built from the caller's library id). */
@Controller

public class FileController {
    private final FileStorageService files;
    private final CurrentUser ctx;


    public FileController(FileStorageService files, CurrentUser ctx) {
        this.files = files;
        this.ctx = ctx;
    }
    @GetMapping("/files/feed/{name}")
    @PreAuthorize(SecurityConfig.NON_GUEST)
    public ResponseEntity<Resource> feedImage(@PathVariable String name) {
        Resource r = files.load(ctx.requireLibraryId(), "feed", name);
        MediaType type = MediaTypeFactory.getMediaType(r).orElse(MediaType.APPLICATION_OCTET_STREAM);
        if (!"image".equals(type.getType())) type = MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff").body(r);
    }
}
