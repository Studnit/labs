package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.domain.StudyGroup;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.StudyGroupService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/groups")
@PreAuthorize(SecurityConfig.NON_GUEST)

public class GroupController {
    private final StudyGroupService groups;
    private final CurrentUser ctx;


    public GroupController(StudyGroupService groups, CurrentUser ctx) {
        this.groups = groups;
        this.ctx = ctx;
    }
    @GetMapping
    public String index(Model m) {
        List<StudyGroup> all = groups.all();
        Map<Long, Long> counts = new HashMap<>(); Set<Long> mine = new HashSet<>();
        for (StudyGroup g : all) { counts.put(g.getId(), groups.memberCount(g.getId())); if (groups.isMember(g.getId())) mine.add(g.getId()); }
        m.addAttribute("groups", all); m.addAttribute("counts", counts); m.addAttribute("mine", mine);
        return "groups";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam(required = false) String description) {
        return "redirect:/groups/" + groups.create(name, description).getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model m) {
        StudyGroup g = groups.get(id);
        m.addAttribute("group", g); m.addAttribute("isMember", groups.isMember(id));
        m.addAttribute("members", groups.membersOf(id)); m.addAttribute("materials", groups.materialsOf(id));
        return "group-detail";
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable Long id) { groups.join(id); return "redirect:/groups/" + id; }

    @PostMapping("/{id}/leave")
    public String leave(@PathVariable Long id, RedirectAttributes ra) { groups.leave(id); ra.addFlashAttribute("success", "You left the group"); return "redirect:/groups"; }

    @PostMapping(value = "/{id}/materials", consumes = "multipart/form-data")
    public String upload(@PathVariable Long id, @RequestParam(required = false) String title, @RequestParam MultipartFile file, RedirectAttributes ra) {
        groups.upload(id, title, file);
        ra.addFlashAttribute("success", "Material shared");
        return "redirect:/groups/" + id;
    }

    @GetMapping("/materials/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        StudyGroupService.Download d = groups.download(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.filename()).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(d.resource());
    }

    @PostMapping("/materials/{id}/delete")
    public String deleteMaterial(@PathVariable Long id, @RequestParam Long groupId, RedirectAttributes ra) {
        groups.deleteMaterial(id);
        ra.addFlashAttribute("success", "Material deleted");
        return "redirect:/groups/" + groupId;
    }
}
