package com.smartlib.web;

import com.smartlib.repo.AuditLogRepository;
import com.smartlib.service.LibraryAdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Platform-level management (secured by /super/** => ROLE_SUPER_ADMIN). */
@Controller
@RequestMapping("/super")

public class SuperAdminController {
    private final LibraryAdminService admin;
    private final AuditLogRepository audit;


    public SuperAdminController(LibraryAdminService admin, AuditLogRepository audit) {
        this.admin = admin;
        this.audit = audit;
    }
    @GetMapping("/libraries")
    public String libraries(Model m) { m.addAttribute("libraries", admin.all()); return "super-libraries"; }

    @PostMapping("/libraries")
    public String create(@RequestParam String name, @RequestParam String slug, @RequestParam String adminName,
                  @RequestParam String adminEmail, @RequestParam String adminPassword, RedirectAttributes ra) {
        if (adminPassword.length() < 8) throw new BusinessException("Admin password must be at least 8 characters");
        admin.create(name, slug, adminName, adminEmail, adminPassword);
        ra.addFlashAttribute("success", "Library created with its admin account");
        return "redirect:/super/libraries";
    }

    @PostMapping("/libraries/{id}/active")
    public String active(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        admin.setActive(id, active);
        ra.addFlashAttribute("success", active ? "Library activated" : "Library deactivated");
        return "redirect:/super/libraries";
    }

    @GetMapping("/audit")
    public String audit(Model m) { m.addAttribute("logs", audit.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 300))); return "audit"; }
}
