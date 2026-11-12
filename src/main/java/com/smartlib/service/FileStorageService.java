package com.smartlib.service;

import com.smartlib.config.AppProperties;
import com.smartlib.web.BusinessException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stores uploads under {upload-dir}/{libraryId}/{module}/{uuid.ext}. Names are server-generated (no traversal). */
@Service
public class FileStorageService {
    public static final Set<String> IMAGES = Set.of("jpg", "jpeg", "png", "gif", "webp");
    public static final Set<String> DOCUMENTS = Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "png", "jpg", "jpeg");
    private static final Pattern SAFE = Pattern.compile("[a-f0-9\\-]{36}\\.[a-z0-9]{2,5}");
    private static final Pattern MODULE = Pattern.compile("[a-z]{3,12}");
    private final Path root;

    public FileStorageService(AppProperties props) { this.root = Paths.get(props.uploadDir()).toAbsolutePath().normalize(); }

    public String save(MultipartFile file, Long libraryId, String module, Set<String> allowedExt) {
        if (file == null || file.isEmpty()) throw new BusinessException("No file selected");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!allowedExt.contains(ext)) throw new BusinessException("File type not allowed: ." + ext);
        if (allowedExt == IMAGES && (file.getContentType() == null || !file.getContentType().startsWith("image/")))
            throw new BusinessException("Only image files are allowed");
        if (IMAGES.contains(ext) && !looksLikeImage(file, ext)) throw new BusinessException("Only image files are allowed");
        String stored = UUID.randomUUID() + "." + ext;
        try {
            Path dir = root.resolve(String.valueOf(libraryId)).resolve(module).normalize();
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) { Files.copy(in, dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { throw new BusinessException("Could not store file"); }
        return stored;
    }

    /** Checks the real file header (magic bytes) so a renamed script/HTML file cannot pass as an image. */
    static boolean looksLikeImage(MultipartFile file, String ext) {
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            return headerMatches(h, ext);
        } catch (IOException e) { return false; }
    }
    static boolean headerMatches(byte[] h, String ext) {
        if (h.length < 4) return false;
        boolean jpg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        boolean png = (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
        boolean gif = h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
        boolean webp = h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
        return switch (ext) { case "jpg", "jpeg" -> jpg; case "png" -> png; case "gif" -> gif; case "webp" -> webp; default -> false; };
    }

    public Resource load(Long libraryId, String module, String storedName) {
        if (!SAFE.matcher(storedName).matches() || !MODULE.matcher(module).matches()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Path p = root.resolve(String.valueOf(libraryId)).resolve(module).resolve(storedName).normalize();
        if (!p.startsWith(root) || !Files.isRegularFile(p)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new FileSystemResource(p);
    }
}
