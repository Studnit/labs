package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.repo.*;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service

public class ChatService {
    public record RoomView(Long id, String name, ChatType type, String last) {}
    public record MessageView(Long id, Long roomId, Long senderId, String sender, String content, String at) {}
    public record Sent(MessageView message, List<Long> recipients) {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm");
    private final ChatRoomRepository rooms;
    private final ChatMemberRepository members;
    private final ChatMessageRepository messages;
    private final AppUserRepository users;
    private final LibraryRepository libraries;
    private final CurrentUser ctx;


    public ChatService(ChatRoomRepository rooms, ChatMemberRepository members, ChatMessageRepository messages, AppUserRepository users, LibraryRepository libraries, CurrentUser ctx) {
        this.rooms = rooms;
        this.members = members;
        this.messages = messages;
        this.users = users;
        this.libraries = libraries;
        this.ctx = ctx;
    }
    private static boolean canChat(AppUser u) { return u.getRole() != Role.GUEST && u.getRole() != Role.SUPER_ADMIN && u.getStatus() == UserStatus.ACTIVE; }

    private AppUser peer(Long id, Long libId) {
        AppUser u = users.findByIdAndLibraryId(id, libId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!canChat(u)) throw new BusinessException("That user cannot be messaged");
        return u;
    }

    @Transactional(readOnly = true)
    public List<RoomView> myRooms() {
        Long me = ctx.id(), lib = ctx.requireLibraryId();
        List<RoomView> out = new ArrayList<>();
        for (ChatMember cm : members.findRoomsOfUser(me, lib)) {
            ChatRoom r = cm.getRoom();
            String name = r.getName();
            if (r.getType() == ChatType.DIRECT)
                name = members.findByRoomId(r.getId()).stream().map(ChatMember::getUser).filter(u -> !u.getId().equals(me)).map(AppUser::getFullName).findFirst().orElse("Direct chat");
            String last = messages.findFirstByRoomIdOrderBySentAtDesc(r.getId()).map(ChatMessage::getContent).orElse("");
            out.add(new RoomView(r.getId(), name, r.getType(), last.length() > 60 ? last.substring(0, 60) + "…" : last));
        }
        return out;
    }

    @Transactional
    public Long direct(Long otherId) {
        Long me = ctx.id(), lib = ctx.requireLibraryId();
        if (me.equals(otherId)) throw new BusinessException("You cannot message yourself");
        AppUser other = peer(otherId, lib);
        AppUser self = ctx.user();
        if (!canChat(self)) throw new BusinessException("Chat is not available for your account");
        String key = Math.min(me, otherId) + "-" + Math.max(me, otherId);
        return rooms.findByLibraryIdAndDirectKey(lib, key).map(ChatRoom::getId).orElseGet(() -> {
            ChatRoom r = new ChatRoom(); r.setLibrary(self.getLibrary()); r.setType(ChatType.DIRECT); r.setDirectKey(key);
            r = rooms.save(r); addMember(r, self); addMember(r, other);
            return r.getId();
        });
    }

    @Transactional
    public Long createGroup(String name, Collection<Long> memberIds) {
        if (name == null || name.isBlank()) throw new BusinessException("Group name is required");
        Long lib = ctx.requireLibraryId();
        AppUser self = ctx.user();
        if (!canChat(self)) throw new BusinessException("Chat is not available for your account");
        ChatRoom r = new ChatRoom(); r.setLibrary(self.getLibrary()); r.setType(ChatType.GROUP); r.setName(name.trim());
        r = rooms.save(r);
        addMember(r, self);
        Set<Long> seen = new HashSet<>(List.of(self.getId()));
        if (memberIds != null) for (Long id : memberIds) if (seen.add(id)) addMember(r, peer(id, lib));
        return r.getId();
    }

    /** Used by study groups: dedicated room, members managed by the group. */
    @Transactional
    public ChatRoom createRoomFor(Library lib, String name) {
        ChatRoom r = new ChatRoom(); r.setLibrary(lib); r.setType(ChatType.GROUP); r.setName(name);
        return rooms.save(r);
    }
    @Transactional
    public void addMember(ChatRoom r, AppUser u) {
        if (!members.existsByRoomIdAndUserId(r.getId(), u.getId())) { ChatMember m = new ChatMember(); m.setRoom(r); m.setUser(u); members.save(m); }
    }
    @Transactional
    public void removeMember(Long roomId, Long userId) { members.findByRoomIdAndUserId(roomId, userId).ifPresent(members::delete); }

    private MessageView view(ChatMessage m) {
        return new MessageView(m.getId(), m.getRoom().getId(), m.getSender().getId(), m.getSender().getFullName(), m.getContent(), m.getSentAt().format(FMT));
    }

    @Transactional(readOnly = true)
    public List<MessageView> history(Long roomId) {
        requireMember(roomId, ctx.id());
        List<ChatMessage> list = new ArrayList<>(messages.findByRoomIdOrderBySentAtDesc(roomId, PageRequest.of(0, 60)));
        Collections.reverse(list);
        return list.stream().map(this::view).toList();
    }

    private void requireMember(Long roomId, Long userId) {
        if (!members.existsByRoomIdAndUserId(roomId, userId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    /** Persist a message and return who should receive it. Used by REST and the WebSocket handler. */
    @Transactional
    public Sent send(Long roomId, Long senderId, String content) {
        if (content == null || content.isBlank()) throw new BusinessException("Message is empty");
        if (content.length() > 2000) throw new BusinessException("Message is too long");
        AppUser sender = users.findById(senderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!canChat(sender)) throw new BusinessException("Chat is not available for your account");
        ChatRoom room = rooms.findByIdAndLibraryId(roomId, sender.libraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireMember(roomId, senderId);
        ChatMessage m = new ChatMessage(); m.setRoom(room); m.setSender(sender); m.setContent(content.trim());
        m = messages.save(m);
        List<Long> recipients = members.findByRoomId(roomId).stream().map(x -> x.getUser().getId()).toList();
        return new Sent(view(m), recipients);
    }
}
