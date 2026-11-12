package com.smartlib.web;

import com.smartlib.config.AppProperties;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.GuestService;
import com.smartlib.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller

public class AuthController {
    private final CurrentUser ctx;
    private final LibraryRepository libraries;
    private final UserService users;
    private final GuestService guests;
    private final AppProperties props;


    public AuthController(CurrentUser ctx, LibraryRepository libraries, UserService users, GuestService guests, AppProperties props) {
        this.ctx = ctx;
        this.libraries = libraries;
        this.users = users;
        this.guests = guests;
        this.props = props;
    }
    @GetMapping("/")
    public String home(Model m) {
        if (ctx.authenticated()) return "redirect:/dashboard";
        m.addAttribute("libraries", libraries.findByActiveTrueOrderByName());
        return "index";
    }

    @GetMapping("/login")
    public String login() { return ctx.authenticated() ? "redirect:/dashboard" : "login"; }

    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String library, Model m) {
        Forms.Register f = new Forms.Register();
        if (library != null) libraries.findBySlug(library).ifPresent(l -> f.setLibraryId(l.getId()));
        m.addAttribute("form", f);
        m.addAttribute("libraries", libraries.findByActiveTrueOrderByName());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") Forms.Register f, BindingResult br, Model m, RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                users.register(f.getLibraryId(), f.getFullName(), f.getEmail(), f.getPhone(), f.getPassword(), f.isGuest());
                ra.addFlashAttribute("success", f.isGuest() ? "Guest account created — you can sign in now."
                        : "Registration received. You can sign in once the library approves your membership.");
                return "redirect:/login";
            } catch (BusinessException e) { m.addAttribute("error", e.getMessage()); }
        }
        m.addAttribute("libraries", libraries.findByActiveTrueOrderByName());
        return "register";
    }

    /** Public visitor registration page, opened from a QR code at the entrance. */
    @GetMapping("/g/{slug}")
    public String guestForm(@PathVariable String slug, Model m) {
        m.addAttribute("lib", libraries.findBySlug(slug).filter(l -> l.isActive()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "guest-form";
    }

    @PostMapping("/g/{slug}")
    public String guestSubmit(@PathVariable String slug, @RequestParam String name, @RequestParam(required = false) String phone,
                       @RequestParam(required = false) String email, @RequestParam(required = false) String purpose,
                       @RequestParam(required = false) String visiting, RedirectAttributes ra) {
        guests.register(slug, name, phone, email, purpose, visiting);
        ra.addFlashAttribute("success", "Thank you, you're checked in. Enjoy your visit!");
        return "redirect:/g/" + slug;
    }
}
