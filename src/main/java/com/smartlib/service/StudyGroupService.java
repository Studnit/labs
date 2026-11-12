package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.repo.*;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service

public class StudyGroupService {
    private final StudyGroupRepository groups;
    private final StudyGroupMemberRepository members;
    private final StudyMaterialRepository materials;
    private final LibraryRepository libraries;
    private final ChatService chat;
    private final FileStorageService files;
    private final AuditService audit;
    private final CurrentUser ctx;


    public StudyGroupService(StudyGroupRepository groups, StudyGroupMemberRepository members, StudyMaterialRepository materials, LibraryRepository libraries, ChatService chat, FileStorageService files, AuditService audit, CurrentUser ctx) {
        this.groups = groups;
        this.members = members;
        this.materials = materials;
        this.libraries = libraries;
        this.chat = chat;
        this.files = files;
        this.audit = audit;
        this.ctx = ctx;
    }
    @Transactional(readOnly = true) public List<StudyGroup> all() { return groups.findByLibraryIdOrderByName(ctx.requireLibraryId()); }
    @Transactional(readOnly = true) public long memberCount(Long gid) { return members.countByGroupId(gid); }
    @Transactional(readOnly = true) public boolean isMember(Long gid) { return members.existsByGroupIdAndUserId(gid, ctx.id()); }
    @Transactional(readOnly = true) public StudyGroup get(Long id) {
        return groups.findByIdAndLibraryId(id, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @Transactional(readOnly = true) public List<StudyGroupMember> membersOf(Long gid) { get(gid); return members.findByGroupId(gid); }
    @Transactional(readOnly = true) public List<StudyMaterial> materialsOf(Long gid) { get(gid); return materials.findByGroupIdOrderByCreatedAtDesc(gid); }

    @Transactional
    public StudyGroup create(String name, String description) {
        if (name == null || name.isBlank()) throw new BusinessException("Group name is required");
        AppUser me = ctx.user();
        StudyGroup g = new StudyGroup();
        g.setLibrary(libraries.getReferenceById(ctx.requireLibraryId())); g.setOwner(me); g.setName(name.trim()); g.setDescription(description);
        g.setChatRoom(chat.createRoomFor(g.getLibrary(), "Study: " + name.trim()));
        g = groups.save(g);
        join(g, me);
        return g;
    }

    private void join(StudyGroup g, AppUser u) {
        if (!members.existsByGroupIdAndUserId(g.getId(), u.getId())) { StudyGroupMember m = new StudyGroupMember(); m.setGroup(g); m.setUser(u); members.save(m); }
        if (g.getChatRoom() != null) chat.addMember(g.getChatRoom(), u);
    }

    @Transactional public void join(Long gid) { join(get(gid), ctx.user()); }

    @Transactional
    public void leave(Long gid) {
        StudyGroup g = get(gid);
        if (g.getOwner().getId().equals(ctx.id())) throw new BusinessException("Owners cannot leave their own group");
        members.findByGroupIdAndUserId(gid, ctx.id()).ifPresent(members::delete);
        if (g.getChatRoom() != null) chat.removeMember(g.getChatRoom().getId(), ctx.id());
    }

    @Transactional
    public void upload(Long gid, String title, MultipartFile file) {
        StudyGroup g = get(gid);
        if (!members.existsByGroupIdAndUserId(gid, ctx.id())) throw new BusinessException("Join the group to share materials");
        String stored = files.save(file, ctx.requireLibraryId(), "groups", FileStorageService.DOCUMENTS);
        StudyMaterial m = new StudyMaterial();
        m.setGroup(g); m.setUploader(ctx.user()); m.setStoredName(stored); m.setOriginalName(file.getOriginalFilename()); m.setSizeBytes(file.getSize());
        m.setTitle(title == null || title.isBlank() ? String.valueOf(file.getOriginalFilename()) : title.trim());
        materials.save(m);
    }

    public record Download(Resource resource, String filename) {}

    @Transactional(readOnly = true)
    public Download download(Long materialId) {
        StudyMaterial m = materials.findById(materialId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        StudyGroup g = get(m.getGroup().getId());
        if (!members.existsByGroupIdAndUserId(g.getId(), ctx.id()) && !ctx.staff()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String name = m.getOriginalName() == null ? m.getStoredName() : m.getOriginalName().replaceAll("[^A-Za-z0-9._ -]", "_");
        return new Download(files.load(ctx.requireLibraryId(), "groups", m.getStoredName()), name);
    }

    @Transactional
    public void deleteMaterial(Long materialId) {
        StudyMaterial m = materials.findById(materialId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        get(m.getGroup().getId());
        if (!ctx.staff() && !m.getUploader().getId().equals(ctx.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        materials.delete(m);
        audit.log("MATERIAL_DELETED", m.getTitle());
    }
}
