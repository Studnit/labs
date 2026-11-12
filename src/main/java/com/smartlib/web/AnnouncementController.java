package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.service.AnnouncementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/announcements")

public class AnnouncementController {
    private final AnnouncementService announcements;


    public AnnouncementController(AnnouncementService announcements) {
        this.announcements = announcements;
    }
    @GetMapping
    public String index(Model m) { m.addAttribute("items", announcements.list(50)); return "announcements"; }

    @PostMapping
    @PreAuthorize(SecurityConfig.STAFF)
    public String post(@RequestParam String title, @RequestParam String body, @RequestParam(defaultValue = "false") boolean pinned, RedirectAttributes ra) {
        announcements.post(title, body, pinned);
        ra.addFlashAttribute("success", "Announcement published and members notified");
        return "redirect:/announcements";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize(SecurityConfig.STAFF)
    public String delete(@PathVariable Long id) { announcements.delete(id); return "redirect:/announcements"; }
}
