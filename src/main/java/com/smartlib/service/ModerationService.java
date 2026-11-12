package com.smartlib.service;

import com.smartlib.domain.*;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.*;
import com.smartlib.security.CurrentUser;
import com.smartlib.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service

public class ModerationService {
    private final ContentReportRepository reports;
    private final PostRepository posts;
    private final PostCommentRepository comments;
    private final ForumQuestionRepository questions;
    private final ForumAnswerRepository answers;
    private final LibraryRepository libraries;
    private final FeatureService features;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CurrentUser ctx;


    public ModerationService(ContentReportRepository reports, PostRepository posts, PostCommentRepository comments, ForumQuestionRepository questions, ForumAnswerRepository answers, LibraryRepository libraries, FeatureService features, NotificationService notifications, AuditService audit, CurrentUser ctx) {
        this.reports = reports;
        this.posts = posts;
        this.comments = comments;
        this.questions = questions;
        this.answers = answers;
        this.libraries = libraries;
        this.features = features;
        this.notifications = notifications;
        this.audit = audit;
        this.ctx = ctx;
    }
    private Feature moduleOf(ReportTarget t) { return t == ReportTarget.POST || t == ReportTarget.COMMENT ? Feature.COMMUNITY_FEED : Feature.DISCUSSION_FORUM; }

    private String text(ReportTarget type, Long id) {
        Long lib = ctx.requireLibraryId();
        return switch (type) {
            case POST -> posts.findByIdAndLibraryId(id, lib).map(Post::getContent).orElse(null);
            case COMMENT -> comments.findById(id).filter(c -> c.getPost().getLibrary().getId().equals(lib)).map(PostComment::getContent).orElse(null);
            case QUESTION -> questions.findByIdAndLibraryId(id, lib).map(q -> q.getTitle() + " — " + q.getBody()).orElse(null);
            case ANSWER -> answers.findById(id).filter(a -> a.getQuestion().getLibrary().getId().equals(lib)).map(ForumAnswer::getBody).orElse(null);
        };
    }

    private void setHidden(ReportTarget type, Long id, boolean hidden) {
        Long lib = ctx.requireLibraryId();
        switch (type) {
            case POST -> posts.findByIdAndLibraryId(id, lib).ifPresent(p -> p.setHidden(hidden));
            case COMMENT -> comments.findById(id).filter(c -> c.getPost().getLibrary().getId().equals(lib)).ifPresent(c -> c.setHidden(hidden));
            case QUESTION -> questions.findByIdAndLibraryId(id, lib).ifPresent(q -> q.setHidden(hidden));
            case ANSWER -> answers.findById(id).filter(a -> a.getQuestion().getLibrary().getId().equals(lib)).ifPresent(a -> a.setHidden(hidden));
        }
    }

    @Transactional
    public void report(ReportTarget type, Long targetId, String reason) {
        Long lib = ctx.requireLibraryId();
        features.require(lib, moduleOf(type));
        String text = text(type, targetId);
        if (text == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        ContentReport r = new ContentReport();
        r.setLibrary(libraries.getReferenceById(lib)); r.setReporter(ctx.user()); r.setTargetType(type); r.setTargetId(targetId);
        r.setReason(reason == null ? "" : reason.trim()); r.setSnapshot(text.length() > 480 ? text.substring(0, 480) : text);
        reports.save(r);
        notifications.notifyStaff(r.getLibrary(), "Content reported", type + " reported: " + (reason == null ? "" : reason), "/moderation");
    }

    /** Open reports, limited to modules that are currently enabled. */
    @Transactional(readOnly = true)
    public List<ContentReport> open() {
        Long lib = ctx.requireLibraryId();
        return reports.findByLibraryIdAndStatusOrderByCreatedAtDesc(lib, ReportStatus.OPEN).stream()
                .filter(r -> features.enabled(lib, moduleOf(r.getTargetType()))).toList();
    }

    @Transactional
    public void resolve(Long reportId, boolean hide) {
        ContentReport r = reports.findByIdAndLibraryId(reportId, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (r.getStatus() != ReportStatus.OPEN) throw new BusinessException("Already handled");
        if (hide) setHidden(r.getTargetType(), r.getTargetId(), true);
        r.setStatus(hide ? ReportStatus.RESOLVED : ReportStatus.DISMISSED);
        r.setResolvedBy(ctx.id());
        audit.log(hide ? "CONTENT_HIDDEN" : "REPORT_DISMISSED", r.getTargetType() + "#" + r.getTargetId());
    }
}
