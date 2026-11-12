package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.domain.ReportTarget;
import com.smartlib.service.ModerationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/moderation")

public class ModerationController {
    private final ModerationService moderation;


    public ModerationController(ModerationService moderation) {
        this.moderation = moderation;
    }
    @GetMapping
    @PreAuthorize(SecurityConfig.STAFF)
    public String index(Model m) { m.addAttribute("reports", moderation.open()); return "moderation"; }

    @PostMapping("/report")
    @PreAuthorize(SecurityConfig.NON_GUEST)
    public String report(@RequestParam ReportTarget type, @RequestParam Long targetId, @RequestParam(required = false) String reason,
                  HttpServletRequest req, RedirectAttributes ra) {
        moderation.report(type, targetId, reason);
        ra.addFlashAttribute("success", "Thanks — the library team will review this.");
        return "redirect:" + GlobalAdvice.safeBack(req);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize(SecurityConfig.STAFF)
    public String resolve(@PathVariable Long id, @RequestParam boolean hide, RedirectAttributes ra) {
        moderation.resolve(id, hide);
        ra.addFlashAttribute("success", hide ? "Content hidden" : "Report dismissed");
        return "redirect:/moderation";
    }
}
