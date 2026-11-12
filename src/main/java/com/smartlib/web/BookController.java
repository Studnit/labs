package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.BookService;
import com.smartlib.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")

public class BookController {
    private final BookService books;
    private final UserService users;
    private final CurrentUser ctx;


    public BookController(BookService books, UserService users, CurrentUser ctx) {
        this.books = books;
        this.users = users;
        this.ctx = ctx;
    }
    @GetMapping
    public String catalogue(@RequestParam(required = false) String q, Model m) {
        m.addAttribute("books", books.search(q)); m.addAttribute("q", q);
        return "books";
    }

    @PostMapping("/save")
    @PreAuthorize(SecurityConfig.STAFF)
    public String save(@RequestParam(required = false) Long id, @RequestParam String title, @RequestParam(required = false) String author,
                @RequestParam(required = false) String isbn, @RequestParam(required = false) String category,
                @RequestParam(required = false) String shelf, @RequestParam int copies, RedirectAttributes ra) {
        if (title.isBlank()) throw new BusinessException("Title is required");
        books.save(id, title, author, isbn, category, shelf, copies);
        ra.addFlashAttribute("success", "Book saved");
        return "redirect:/books";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize(SecurityConfig.STAFF)
    public String delete(@PathVariable Long id, RedirectAttributes ra) { books.delete(id); ra.addFlashAttribute("success", "Book deleted"); return "redirect:/books"; }

    @GetMapping("/issues")
    @PreAuthorize(SecurityConfig.STAFF)
    public String issues(Model m) {
        m.addAttribute("issues", books.openIssues());
        m.addAttribute("members", users.activeMembers());
        m.addAttribute("books", books.search(""));
        return "book-issues";
    }

    @PostMapping("/issue")
    @PreAuthorize(SecurityConfig.STAFF)
    public String issue(@RequestParam Long bookId, @RequestParam Long memberId, @RequestParam(required = false) Integer days, RedirectAttributes ra) {
        var i = books.issue(bookId, memberId, days);
        ra.addFlashAttribute("success", "Issued — due " + i.getDueDate());
        return "redirect:/books/issues";
    }

    @PostMapping("/issues/{id}/return")
    @PreAuthorize(SecurityConfig.STAFF)
    public String ret(@PathVariable Long id, RedirectAttributes ra) {
        var i = books.returnBook(id);
        ra.addFlashAttribute("success", "Returned" + (i.getFine() != null && i.getFine().signum() > 0 ? " — late fine " + i.getFine() : ""));
        return "redirect:/books/issues";
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('MEMBER')")
    public String mine(Model m) { m.addAttribute("issues", books.myIssues()); return "my-books"; }
}
