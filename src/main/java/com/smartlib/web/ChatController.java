package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.domain.AppUser;
import com.smartlib.domain.Role;
import com.smartlib.domain.UserStatus;
import com.smartlib.repo.AppUserRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/chat")
@PreAuthorize(SecurityConfig.NON_GUEST)

public class ChatController {
    private final ChatService chat;
    private final AppUserRepository users;
    private final CurrentUser ctx;


    public ChatController(ChatService chat, AppUserRepository users, CurrentUser ctx) {
        this.chat = chat;
        this.users = users;
        this.ctx = ctx;
    }
    @GetMapping
    public String index(@RequestParam(required = false) Long room, Model m) {
        List<AppUser> people = users.findByLibraryIdAndRoleInAndStatus(ctx.requireLibraryId(),
                Set.of(Role.MEMBER, Role.LIBRARIAN, Role.LIBRARY_ADMIN), UserStatus.ACTIVE).stream().filter(u -> !u.getId().equals(ctx.id())).toList();
        m.addAttribute("rooms", chat.myRooms()); m.addAttribute("people", people); m.addAttribute("activeRoom", room);
        m.addAttribute("meId", ctx.id());
        return "chat";
    }

    @GetMapping("/api/rooms/{id}/messages")
    @ResponseBody
    public ResponseEntity<List<ChatService.MessageView>> messages(@PathVariable Long id) { return ResponseEntity.ok(chat.history(id)); }

    @PostMapping("/direct/{userId}")
    public String direct(@PathVariable Long userId) { return "redirect:/chat?room=" + chat.direct(userId); }

    @PostMapping("/groups")
    public String group(@RequestParam String name, @RequestParam(required = false) List<Long> memberIds) {
        return "redirect:/chat?room=" + chat.createGroup(name, memberIds);
    }
}
