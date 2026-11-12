package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.*;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/members")
@PreAuthorize(SecurityConfig.STAFF)

public class MemberController {
    private final UserService users;
    private final CurrentUser ctx;
    private final FeatureService features;
    private final SubscriptionRepository subs;
    private final AttendanceRepository attendance;
    private final BookIssueRepository issues;


    public MemberController(UserService users, CurrentUser ctx, FeatureService features, SubscriptionRepository subs, AttendanceRepository attendance, BookIssueRepository issues) {
        this.users = users;
        this.ctx = ctx;
        this.features = features;
        this.subs = subs;
        this.attendance = attendance;
        this.issues = issues;
    }
    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) UserStatus status, Model m) {
        m.addAttribute("members", users.list(q, status));
        m.addAttribute("q", q); m.addAttribute("status", status);
        m.addAttribute("statuses", UserStatus.values());
        return "members";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model m) {
        AppUser u = users.get(id);
        Long lib = ctx.requireLibraryId();
        m.addAttribute("member", u);
        if (features.enabled(lib, Feature.SUBSCRIPTIONS)) m.addAttribute("subscriptions", subs.findByMemberIdOrderByEndDateDesc(id));
        if (features.enabled(lib, Feature.QR_ATTENDANCE)) m.addAttribute("sessions", attendance.findByUserIdOrderByCheckInDesc(id, PageRequest.of(0, 15)));
        if (features.enabled(lib, Feature.BOOK_MANAGEMENT)) m.addAttribute("books", issues.findByMemberIdOrderByIssuedAtDesc(id));
        return "member-detail";
    }

    /** Staff create an already-approved member (a temporary password is shown once). */
    @PostMapping("/create")
    public String create(@RequestParam String fullName, @RequestParam String email, @RequestParam(required = false) String phone, RedirectAttributes ra) {
        String temp = "Lib#" + UUID.randomUUID().toString().substring(0, 8);
        AppUser u = users.register(ctx.requireLibraryId(), fullName, email, phone, temp, false);
        users.setStatus(u.getId(), UserStatus.ACTIVE);
        ra.addFlashAttribute("success", "Member created. Temporary password (share securely, shown once): " + temp);
        return "redirect:/members/" + u.getId();
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam UserStatus status, RedirectAttributes ra) {
        if (status == UserStatus.PENDING) throw new BusinessException("Invalid status");
        users.setStatus(id, status);
        ra.addFlashAttribute("success", "Status updated to " + status);
        return "redirect:/members/" + id;
    }

    @PostMapping("/{id}/decide")
    public String decide(@PathVariable Long id, @RequestParam boolean approve, RedirectAttributes ra) {
        users.setStatus(id, approve ? UserStatus.ACTIVE : UserStatus.REJECTED);
        ra.addFlashAttribute("success", approve ? "Member approved" : "Request rejected");
        return "redirect:/members?status=PENDING";
    }
}
