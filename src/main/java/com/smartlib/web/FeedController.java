package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.service.FeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/feed")
@PreAuthorize(SecurityConfig.NON_GUEST)

public class FeedController {
    private final FeedService feed;


    public FeedController(FeedService feed) {
        this.feed = feed;
    }
    @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page, Model m) {
        m.addAttribute("posts", feed.feed(page)); m.addAttribute("page", page);
        return "feed";
    }

    @PostMapping(consumes = "multipart/form-data")
    public String create(@RequestParam String content, @RequestParam(required = false) MultipartFile image, RedirectAttributes ra) {
        feed.create(content, image);
        ra.addFlashAttribute("success", "Posted");
        return "redirect:/feed";
    }

    @PostMapping("/api/{id}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> like(@PathVariable Long id) { return ResponseEntity.ok(feed.toggleLike(id)); }

    @PostMapping("/{id}/comment")
    public String comment(@PathVariable Long id, @RequestParam String content) {
        feed.comment(id, content);
        return "redirect:/feed#post-" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) { feed.remove(id); ra.addFlashAttribute("success", "Post removed"); return "redirect:/feed"; }

    @PostMapping("/{id}/pin")
    public String pin(@PathVariable Long id) { feed.pin(id); return "redirect:/feed"; }
}
