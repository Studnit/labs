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

import java.util.List;

@Service

public class ForumService {
    public record QuestionRow(ForumQuestion q, long answers) {}

    private final ForumCategoryRepository categories;
    private final ForumQuestionRepository questions;
    private final ForumAnswerRepository answers;
    private final LibraryRepository libraries;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CurrentUser ctx;


    public ForumService(ForumCategoryRepository categories, ForumQuestionRepository questions, ForumAnswerRepository answers, LibraryRepository libraries, NotificationService notifications, AuditService audit, CurrentUser ctx) {
        this.categories = categories;
        this.questions = questions;
        this.answers = answers;
        this.libraries = libraries;
        this.notifications = notifications;
        this.audit = audit;
        this.ctx = ctx;
    }
    @Transactional(readOnly = true) public List<ForumCategory> categories() { return categories.findByLibraryIdOrderByName(ctx.requireLibraryId()); }

    @Transactional
    public void addCategory(String name, String description) {
        if (!ctx.staff()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        ForumCategory c = new ForumCategory();
        c.setLibrary(libraries.getReferenceById(ctx.requireLibraryId())); c.setName(name.trim()); c.setDescription(description);
        categories.save(c);
        audit.log("FORUM_CATEGORY_ADDED", name);
    }

    @Transactional(readOnly = true)
    public List<QuestionRow> list(Long categoryId, int page) {
        Long lib = ctx.requireLibraryId();
        var pr = PageRequest.of(Math.max(page, 0), 20);
        List<ForumQuestion> qs = categoryId == null ? questions.findByLibraryIdAndHiddenFalseOrderByCreatedAtDesc(lib, pr)
                : questions.findByLibraryIdAndCategoryIdAndHiddenFalseOrderByCreatedAtDesc(lib, categoryId, pr);
        return qs.stream().map(q -> new QuestionRow(q, answers.countByQuestionIdAndHiddenFalse(q.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public ForumQuestion question(Long id) {
        return questions.findByIdAndLibraryId(id, ctx.requireLibraryId()).filter(q -> !q.isHidden()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @Transactional(readOnly = true) public List<ForumAnswer> answersOf(Long qid) { return answers.findByQuestionIdAndHiddenFalseOrderByAcceptedDescCreatedAtAsc(qid); }

    @Transactional
    public ForumQuestion ask(Long categoryId, String title, String body) {
        if (title == null || title.isBlank() || body == null || body.isBlank()) throw new BusinessException("Title and details are required");
        ForumQuestion q = new ForumQuestion();
        q.setLibrary(libraries.getReferenceById(ctx.requireLibraryId())); q.setAuthor(ctx.user()); q.setTitle(title.trim()); q.setBody(body.trim());
        if (categoryId != null) q.setCategory(categories.findByIdAndLibraryId(categoryId, ctx.requireLibraryId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return questions.save(q);
    }

    @Transactional
    public void answer(Long qid, String body) {
        if (body == null || body.isBlank()) throw new BusinessException("Answer is empty");
        ForumQuestion q = question(qid);
        ForumAnswer a = new ForumAnswer(); a.setQuestion(q); a.setAuthor(ctx.user()); a.setBody(body.trim());
        answers.save(a);
        if (!q.getAuthor().getId().equals(ctx.id()))
            notifications.notify(q.getAuthor(), "New answer", ctx.details().getFullName() + " answered: " + q.getTitle(), "/forum/q/" + q.getId());
    }

    @Transactional
    public void accept(Long answerId) {
        ForumAnswer a = answers.findById(answerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ForumQuestion q = a.getQuestion();
        if (!q.getLibrary().getId().equals(ctx.requireLibraryId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!ctx.staff() && !q.getAuthor().getId().equals(ctx.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        answers.findByQuestionIdAndHiddenFalseOrderByAcceptedDescCreatedAtAsc(q.getId()).forEach(x -> x.setAccepted(false));
        a.setAccepted(true); q.setResolved(true);
        if (!a.getAuthor().getId().equals(ctx.id()))
            notifications.notify(a.getAuthor(), "Answer accepted", "Your answer to \"" + q.getTitle() + "\" was accepted", "/forum/q/" + q.getId());
    }
}
