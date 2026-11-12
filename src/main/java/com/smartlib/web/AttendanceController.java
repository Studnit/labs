package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.domain.Library;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.AttendanceService;
import com.smartlib.service.LibraryAdminService;
import com.smartlib.service.QrService;
import com.smartlib.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/attendance")

public class AttendanceController {
    private final AttendanceService attendance;
    private final CurrentUser ctx;
    private final UserService users;
    private final LibraryRepository libraries;
    private final LibraryAdminService libraryAdmin;
    private final QrService qr;


    public AttendanceController(AttendanceService attendance, CurrentUser ctx, UserService users, LibraryRepository libraries, LibraryAdminService libraryAdmin, QrService qr) {
        this.attendance = attendance;
        this.ctx = ctx;
        this.users = users;
        this.libraries = libraries;
        this.libraryAdmin = libraryAdmin;
        this.qr = qr;
    }
    @GetMapping
    @PreAuthorize(SecurityConfig.NON_GUEST)
    public String index(Model m) {
        if (ctx.staff()) {
            m.addAttribute("present", attendance.present());
            m.addAttribute("today", attendance.today());
            m.addAttribute("members", users.activeMembers());
            return "attendance-staff";
        }
        m.addAttribute("open", attendance.open(ctx.id()).orElse(null));
        m.addAttribute("history", attendance.history(ctx.id()));
        m.addAttribute("timeStats", attendance.myTimeStats());
        return "attendance-member";
    }

    /** Opened by the phone camera or the in-app scanner; shows a confirmation, the POST does the work. */
    @GetMapping("/scan/{token}")
    @PreAuthorize(SecurityConfig.NON_GUEST)
    public String scanPage(@PathVariable String token, Model m) {
        Library lib = attendance.libraryForToken(token);
        m.addAttribute("lib", lib); m.addAttribute("token", token);
        m.addAttribute("open", attendance.open(ctx.id()).orElse(null));
        return "attendance-scan";
    }

    @PostMapping("/scan/{token}")
    @PreAuthorize("hasRole('MEMBER')")
    public String scan(@PathVariable String token, RedirectAttributes ra) {
        AttendanceService.ScanResult r = attendance.scan(token);
        ra.addFlashAttribute("success", r.checkedIn() ? "Checked in. Happy studying!" : "Checked out after " + r.minutes() + " minutes. See you soon!");
        return "redirect:/attendance";
    }

    @GetMapping("/scanner")
    @PreAuthorize("hasRole('MEMBER')")
    public String scanner() { return "attendance-scanner"; }

    @GetMapping("/qr")
    @PreAuthorize(SecurityConfig.STAFF)
    public String qrPage(Model m) {
        Library lib = libraries.findById(ctx.requireLibraryId()).orElseThrow();
        m.addAttribute("lib", lib); m.addAttribute("scanUrl", qr.attendanceUrl(lib.getQrToken()));
        return "attendance-qr";
    }

    @GetMapping(value = "/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize(SecurityConfig.STAFF)
    public ResponseEntity<byte[]> qrPng() {
        Library lib = libraries.findById(ctx.requireLibraryId()).orElseThrow();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(qr.png(qr.attendanceUrl(lib.getQrToken()), 480));
    }

    @PostMapping("/qr/rotate")
    @PreAuthorize(SecurityConfig.ADMIN)
    public String rotate(RedirectAttributes ra) {
        libraryAdmin.rotateQr();
        ra.addFlashAttribute("success", "QR code regenerated. Print and replace the old one at the entrance.");
        return "redirect:/attendance/qr";
    }

    @PostMapping("/manual/in")
    @PreAuthorize(SecurityConfig.STAFF)
    public String manualIn(@RequestParam Long userId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at,
                    @RequestParam(required = false) String note, RedirectAttributes ra) {
        attendance.manualCheckIn(userId, at, note);
        ra.addFlashAttribute("success", "Member checked in");
        return "redirect:/attendance";
    }

    @PostMapping("/manual/out/{id}")
    @PreAuthorize(SecurityConfig.STAFF)
    public String manualOut(@PathVariable Long id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at, RedirectAttributes ra) {
        attendance.manualCheckOut(id, at);
        ra.addFlashAttribute("success", "Member checked out");
        return "redirect:/attendance";
    }
}
