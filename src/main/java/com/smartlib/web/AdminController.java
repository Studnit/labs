package com.smartlib.web;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.AppUserRepository;
import com.smartlib.repo.AuditLogRepository;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.LibraryAdminService;
import com.smartlib.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/** Library Admin area (secured by /admin/** => ROLE_LIBRARY_ADMIN in SecurityConfig). */
@Controller
@RequestMapping("/admin")

public class AdminController {
    private final LibraryAdminService admin;
    private final FeatureService features;
    private final LibraryRepository libraries;
    private final AppUserRepository users;
    private final UserService userService;
    private final AuditLogRepository audit;
    private final CurrentUser ctx;


    public AdminController(LibraryAdminService admin, FeatureService features, LibraryRepository libraries, AppUserRepository users, UserService userService, AuditLogRepository audit, CurrentUser ctx) {
        this.admin = admin;
        this.features = features;
        this.libraries = libraries;
        this.users = users;
        this.userService = userService;
        this.audit = audit;
        this.ctx = ctx;
    }
    @GetMapping("/features")
    public String features(Model m) {
        m.addAttribute("flags", features.all(ctx.requireLibraryId()));
        return "admin-features";
    }

    @PostMapping("/features/{feature}")
    public String toggle(@PathVariable Feature feature, @RequestParam boolean enabled, RedirectAttributes ra) {
        admin.setFeature(feature, enabled);
        ra.addFlashAttribute("success", feature.getLabel() + (enabled ? " enabled" : " disabled"));
        return "redirect:/admin/features";
    }

    @GetMapping("/library")
    public String library(Model m) { m.addAttribute("lib", libraries.findById(ctx.requireLibraryId()).orElseThrow()); return "admin-library"; }

    @PostMapping("/library")
    public String saveLibrary(@RequestParam String name, @RequestParam(required = false) String address, @RequestParam(required = false) String phone,
                       @RequestParam(required = false) String email, @RequestParam(required = false) String upiId,
                       @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime openTime, @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime closeTime,
                       @RequestParam BigDecimal finePerDay, @RequestParam int maxIssueDays,
                       @RequestParam(required = false) String tagline, @RequestParam(required = false) String facilities, RedirectAttributes ra) {
        admin.updateSettings(name, address, phone, email, upiId, openTime, closeTime, finePerDay, maxIssueDays, tagline, facilities);
        ra.addFlashAttribute("success", "Settings saved");
        return "redirect:/admin/library";
    }

    @GetMapping("/staff")
    public String staff(Model m) {
        m.addAttribute("staff", users.findByLibraryIdAndRoleInAndStatus(ctx.requireLibraryId(), Set.of(Role.LIBRARIAN, Role.LIBRARY_ADMIN), UserStatus.ACTIVE));
        return "admin-staff";
    }

    @PostMapping("/staff")
    public String createStaff(@RequestParam String fullName, @RequestParam String email, @RequestParam String password, @RequestParam Role role, RedirectAttributes ra) {
        if (password.length() < 8) throw new BusinessException("Password must be at least 8 characters");
        userService.createStaff(fullName, email, password, role);
        ra.addFlashAttribute("success", "Staff account created");
        return "redirect:/admin/staff";
    }

    @GetMapping("/audit")
    public String audit(Model m) {
        m.addAttribute("logs", audit.findByLibraryIdOrderByCreatedAtDesc(ctx.requireLibraryId(), PageRequest.of(0, 200)));
        return "audit";
    }
}
