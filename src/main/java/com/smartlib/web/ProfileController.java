package com.smartlib.web;

import com.smartlib.security.CurrentUser;
import com.smartlib.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")

public class ProfileController {
    private final CurrentUser ctx;
    private final UserService users;


    public ProfileController(CurrentUser ctx, UserService users) {
        this.ctx = ctx;
        this.users = users;
    }
    @GetMapping
    public String view(Model m) { m.addAttribute("profile", ctx.user()); return "profile"; }

    @PostMapping
    public String update(@RequestParam String fullName, @RequestParam(required = false) String phone, @RequestParam(required = false) String address,
                  @RequestParam(required = false) String bio, RedirectAttributes ra) {
        if (fullName.isBlank()) throw new BusinessException("Name is required");
        users.updateProfile(fullName, phone, address, bio == null ? null : bio.substring(0, Math.min(bio.length(), 500)));
        ra.addFlashAttribute("success", "Profile updated");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String password(@RequestParam String current, @RequestParam String next, @RequestParam String confirm, RedirectAttributes ra) {
        if (next.length() < 8 || next.length() > 72) throw new BusinessException("New password must be 8-72 characters");
        if (!next.equals(confirm)) throw new BusinessException("Passwords do not match");
        users.changePassword(current, next);
        ra.addFlashAttribute("success", "Password changed");
        return "redirect:/profile";
    }
}
