package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.domain.Feature;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.ExportService;
import com.smartlib.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/reports")
@PreAuthorize(SecurityConfig.STAFF)

public class ReportController {
    private final ReportService reports;
    private final ExportService exports;
    private final FeatureService features;
    private final LibraryRepository libraries;
    private final CurrentUser ctx;


    public ReportController(ReportService reports, ExportService exports, FeatureService features, LibraryRepository libraries, CurrentUser ctx) {
        this.reports = reports;
        this.exports = exports;
        this.features = features;
        this.libraries = libraries;
        this.ctx = ctx;
    }
    @GetMapping
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model m) {
        LocalDate t = to == null ? LocalDate.now() : to, f = from == null ? t.minusDays(29) : from;
        m.addAttribute("summary", reports.summary(f, t));
        Map<String, String> available = new LinkedHashMap<>();
        Long lib = ctx.requireLibraryId();
        for (String type : new String[]{"members", "attendance", "subscriptions", "payments", "books", "guests"})
            if (features.enabled(lib, ReportService.featureFor(type))) available.put(type, type.substring(0, 1).toUpperCase() + type.substring(1));
        m.addAttribute("exportTypes", available);
        return "reports";
    }

    @GetMapping("/export/{type}/{fmt}")
    public ResponseEntity<byte[]> export(@PathVariable String type, @PathVariable String fmt,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate t = to == null ? LocalDate.now() : to, f = from == null ? t.minusDays(29) : from;
        var table = reports.table(type, f, t); // also enforces that the type's module is enabled
        String base = type + "-" + f + "_" + t;
        if ("xlsx".equals(fmt))
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + base + ".xlsx\"").body(exports.xlsx(table));
        if ("pdf".equals(fmt)) {
            String name = libraries.findById(ctx.requireLibraryId()).orElseThrow().getName();
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + base + ".pdf\"").body(exports.pdf(table, name));
        }
        throw new BusinessException("Unsupported format");
    }
}
