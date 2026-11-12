package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.repo.*;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service

public class FeedService {
    public record PostView(Post post, long likes, boolean liked, List<PostComment> comments) {}

    private final PostRepository posts;
    private final PostLikeRepository likes;
    private final PostCommentRepository comments;
    private final LibraryRepository libraries;
    private final FileStorageService files;
    private final NotificationService notifications;
    private final CurrentUser ctx;


    public FeedService(PostRepository posts, PostLikeRepository likes, PostCommentRepository comments, LibraryRepository libraries, FileStorageService files, NotificationService notifications, CurrentUser ctx) {
        this.posts = posts;
        this.likes = likes;
        this.comments = comments;
        this.libraries = libraries;
        this.files = files;
        this.notifications = notifications;
        this.ctx = ctx;
    }
    @Transactional(readOnly = true)
    public List<PostView> feed(int page) {
        Long lib = ctx.requireLibraryId();
        return posts.findByLibraryIdAndHiddenFalseOrderByPinnedDescCreatedAtDesc(lib, PageRequest.of(Math.max(page, 0), 10)).stream()
                .map(p -> new PostView(p, likes.countByPostId(p.getId()), likes.existsByPostIdAndUserId(p.getId(), ctx.id()),
                        comments.findByPostIdAndHiddenFalseOrderByCreatedAt(p.getId()))).toList();
    }

    private Post post(Long id) { return posts.findByIdAndLibraryId(id, ctx.requireLibraryId()).filter(p -> !p.isHidden()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }

    @Transactional
    public Post create(String content, MultipartFile image) {
        if (content == null || content.isBlank()) throw new BusinessException("Write something first");
        Post p = new Post();
        p.setLibrary(libraries.getReferenceById(ctx.requireLibraryId())); p.setAuthor(ctx.user()); p.setContent(content.trim());
        if (image != null && !image.isEmpty()) p.setImageName(files.save(image, ctx.requireLibraryId(), "feed", FileStorageService.IMAGES));
        return posts.save(p);
    }

    @Transactional
    public Map<String, Object> toggleLike(Long postId) {
        Post p = post(postId);
        var existing = likes.findByPostIdAndUserId(postId, ctx.id());
        boolean liked;
        if (existing.isPresent()) { likes.delete(existing.get()); liked = false; }
        else {
            PostLike l = new PostLike(); l.setPost(p); l.setUser(ctx.user()); likes.save(l); liked = true;
            if (!p.getAuthor().getId().equals(ctx.id()))
                notifications.notify(p.getAuthor(), "New like", ctx.details().getFullName() + " liked your post", "/feed");
        }
        return Map.of("liked", liked, "likes", likes.countByPostId(postId));
    }

    @Transactional
    public PostComment comment(Long postId, String content) {
        if (content == null || content.isBlank()) throw new BusinessException("Comment is empty");
        Post p = post(postId);
        PostComment c = new PostComment(); c.setPost(p); c.setAuthor(ctx.user()); c.setContent(content.trim());
        c = comments.save(c);
        if (!p.getAuthor().getId().equals(ctx.id()))
            notifications.notify(p.getAuthor(), "New comment", ctx.details().getFullName() + " commented on your post", "/feed");
        return c;
    }

    /** Authors remove their own posts; staff can remove any (soft delete so reports keep their evidence). */
    @Transactional
    public void remove(Long postId) {
        Post p = posts.findByIdAndLibraryId(postId, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ctx.staff() && !p.getAuthor().getId().equals(ctx.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        p.setHidden(true);
    }

    @Transactional
    public void pin(Long postId) {
        if (!ctx.staff()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Post p = post(postId); p.setPinned(!p.isPinned());
    }
}
