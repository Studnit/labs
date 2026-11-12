package com.smartlib.web;

import com.smartlib.config.AppProperties;
import com.smartlib.config.SecurityConfig;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.GuestService;
import com.smartlib.service.QrService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/guests")
@PreAuthorize(SecurityConfig.STAFF)

public class GuestController {
    private final GuestService guests;
    private final LibraryRepository libraries;
    private final CurrentUser ctx;
    private final QrService qr;
    private final AppProperties props;


    public GuestController(GuestService guests, LibraryRepository libraries, CurrentUser ctx, QrService qr, AppProperties props) {
        this.guests = guests;
        this.libraries = libraries;
        this.ctx = ctx;
        this.qr = qr;
        this.props = props;
    }
    private String url() { return props.baseUrl().replaceAll("/+$", "") + "/g/" + libraries.findById(ctx.requireLibraryId()).orElseThrow().getSlug(); }

    @GetMapping
    public String index(Model m) { m.addAttribute("guests", guests.recent()); m.addAttribute("registrationUrl", url()); return "guests"; }

    @PostMapping
    public String add(@RequestParam String name, @RequestParam(required = false) String phone, @RequestParam(required = false) String purpose, RedirectAttributes ra) {
        guests.addByStaff(name, phone, purpose);
        ra.addFlashAttribute("success", "Guest registered");
        return "redirect:/guests";
    }

    @PostMapping("/{id}/checkout")
    public String checkout(@PathVariable Long id) { guests.checkOut(id); return "redirect:/guests"; }

    @GetMapping(value = "/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrPng() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(qr.png(url(), 400)); }
}
