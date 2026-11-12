package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.domain.*;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/payments")
@PreAuthorize(SecurityConfig.NON_GUEST)

public class PaymentController {
    private final PaymentService payments;
    private final SubscriptionService subs;
    private final UserService users;
    private final LibraryRepository libraries;
    private final ExportService exports;
    private final QrService qr;
    private final CurrentUser ctx;


    public PaymentController(PaymentService payments, SubscriptionService subs, UserService users, LibraryRepository libraries, ExportService exports, QrService qr, CurrentUser ctx) {
        this.payments = payments;
        this.subs = subs;
        this.users = users;
        this.libraries = libraries;
        this.exports = exports;
        this.qr = qr;
        this.ctx = ctx;
    }
    @GetMapping
    public String index(Model m) {
        if (ctx.staff()) {
            m.addAttribute("payments", payments.recent());
            m.addAttribute("members", users.activeMembers());
            m.addAttribute("methods", new PaymentMethod[]{PaymentMethod.CASH, PaymentMethod.UPI});
            m.addAttribute("plans", subs.plans());
            return "payments-staff";
        }
        m.addAttribute("payments", payments.mine());
        return "payments-member";
    }

    @PostMapping("/collect")
    @PreAuthorize(SecurityConfig.STAFF)
    public String collect(@RequestParam Long memberId, @RequestParam(required = false) Long planId, @RequestParam(required = false) BigDecimal amount,
                   @RequestParam PaymentMethod method, @RequestParam(required = false) String reference,
                   @RequestParam(required = false) String purpose, RedirectAttributes ra) {
        Payment p = payments.collect(memberId, planId, amount, method, reference, purpose);
        ra.addFlashAttribute("success", "Payment recorded — receipt " + p.getReceiptNo());
        return "redirect:/payments";
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize(SecurityConfig.STAFF)
    public String verify(@PathVariable Long id, @RequestParam boolean approve, RedirectAttributes ra) {
        payments.verify(id, approve);
        ra.addFlashAttribute("success", approve ? "Payment verified" : "Payment rejected");
        return "redirect:/payments";
    }

    @GetMapping("/checkout")
    @PreAuthorize("hasRole('MEMBER')")
    public String checkoutPage(@RequestParam Long planId, Model m) {
        payments.assertCanBuy();
        SubscriptionPlan plan = subs.plan(planId);
        m.addAttribute("plan", plan);
        m.addAttribute("upiId", libraries.findById(ctx.requireLibraryId()).map(Library::getUpiId).orElse(null));
        return "checkout";
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('MEMBER')")
    public String checkout(@RequestParam Long planId, @RequestParam PaymentMethod method, @RequestParam(required = false) String reference, RedirectAttributes ra) {
        Payment p = payments.checkout(planId, method, reference);
        if (method == PaymentMethod.ONLINE) return "redirect:/payments/" + p.getId() + "/gateway";
        ra.addFlashAttribute("success", "UPI payment submitted. The library will verify it shortly.");
        return "redirect:/payments";
    }

    @GetMapping("/{id}/gateway")
    @PreAuthorize("hasRole('MEMBER')")
    public String gateway(@PathVariable Long id, Model m) {
        Payment p = payments.get(id);
        if (p.getStatus() != PaymentStatus.PENDING || p.getMethod() != PaymentMethod.ONLINE) return "redirect:/payments";
        m.addAttribute("payment", p);
        return "gateway";
    }

    @PostMapping("/{id}/gateway")
    @PreAuthorize("hasRole('MEMBER')")
    public String gatewayConfirm(@PathVariable Long id, RedirectAttributes ra) {
        Payment p = payments.gatewayConfirm(id);
        ra.addFlashAttribute("success", "Payment successful — receipt " + p.getReceiptNo());
        return "redirect:/payments";
    }

    @GetMapping(value = "/upi-qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<byte[]> upiQr(@RequestParam Long planId) {
        SubscriptionPlan plan = subs.plan(planId);
        Library lib = libraries.findById(ctx.requireLibraryId()).orElseThrow();
        if (lib.getUpiId() == null || lib.getUpiId().isBlank()) throw new BusinessException("UPI is not configured for this library");
        String uri = "upi://pay?pa=" + enc(lib.getUpiId()) + "&pn=" + enc(lib.getName()) + "&am=" + plan.getPrice().toPlainString() + "&cu=INR&tn=" + enc(plan.getName());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(qr.png(uri, 320));
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    @GetMapping(value = "/{id}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receipt(@PathVariable Long id) {
        Payment p = payments.receipt(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + p.getReceiptNo() + ".pdf\"")
                .body(exports.receiptPdf(p));
    }
}
