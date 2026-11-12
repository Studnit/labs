package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.service.ForumService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/forum")
@PreAuthorize(SecurityConfig.NON_GUEST)

public class ForumController {
    private final ForumService forum;


    public ForumController(ForumService forum) {
        this.forum = forum;
    }
    @GetMapping
    public String index(@RequestParam(required = false) Long category, @RequestParam(defaultValue = "0") int page, Model m) {
        m.addAttribute("categories", forum.categories()); m.addAttribute("rows", forum.list(category, page));
        m.addAttribute("category", category); m.addAttribute("page", page);
        return "forum";
    }

    @GetMapping("/q/{id}")
    public String question(@PathVariable Long id, Model m) {
        var q = forum.question(id);
        m.addAttribute("q", q); m.addAttribute("answers", forum.answersOf(id));
        return "forum-question";
    }

    @PostMapping("/ask")
    public String ask(@RequestParam(required = false) Long categoryId, @RequestParam String title, @RequestParam String body) {
        return "redirect:/forum/q/" + forum.ask(categoryId, title, body).getId();
    }

    @PostMapping("/q/{id}/answer")
    public String answer(@PathVariable Long id, @RequestParam String body) { forum.answer(id, body); return "redirect:/forum/q/" + id; }

    @PostMapping("/q/{qid}/accept/{answerId}")
    public String accept(@PathVariable Long qid, @PathVariable Long answerId) { forum.accept(answerId); return "redirect:/forum/q/" + qid; }

    @PostMapping("/categories")
    public String category(@RequestParam String name, @RequestParam(required = false) String description, RedirectAttributes ra) {
        forum.addCategory(name, description);
        ra.addFlashAttribute("success", "Category added");
        return "redirect:/forum";
    }
}
