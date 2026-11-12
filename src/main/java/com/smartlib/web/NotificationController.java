package com.smartlib.web;

import com.smartlib.security.CurrentUser;
import com.smartlib.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notifications")

public class NotificationController {
    private final NotificationService notifications;
    private final CurrentUser ctx;


    public NotificationController(NotificationService notifications, CurrentUser ctx) {
        this.notifications = notifications;
        this.ctx = ctx;
    }
    @GetMapping
    public String index(Model m) { m.addAttribute("items", notifications.recent(ctx.id())); return "notifications"; }

    @PostMapping("/read")
    public String readAll() { notifications.markAllSeen(ctx.id()); return "redirect:/notifications"; }

    @PostMapping("/{id}/open")
    public String open(@PathVariable Long id, @RequestParam(required = false) String to) {
        notifications.markSeen(ctx.id(), id);
        // Only internal paths are allowed as redirect targets.
        boolean internal = to != null && to.startsWith("/") && !to.startsWith("//") && !to.contains("\\");
        return "redirect:" + (internal ? to : "/notifications");
    }
}
