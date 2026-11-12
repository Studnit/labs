package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.domain.PaymentMethod;
import com.smartlib.feature.FeatureService;
import com.smartlib.domain.Feature;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.PaymentService;
import com.smartlib.service.SubscriptionService;
import com.smartlib.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/subscriptions")
@PreAuthorize(SecurityConfig.NON_GUEST)

public class SubscriptionController {
    private final SubscriptionService subs;
    private final PaymentService payments;
    private final UserService users;
    private final FeatureService features;
    private final CurrentUser ctx;


    public SubscriptionController(SubscriptionService subs, PaymentService payments, UserService users, FeatureService features, CurrentUser ctx) {
        this.subs = subs;
        this.payments = payments;
        this.users = users;
        this.features = features;
        this.ctx = ctx;
    }
    @GetMapping
    public String index(Model m) {
        if (ctx.staff()) {
            m.addAttribute("subscriptions", subs.all());
            m.addAttribute("expiring", subs.expiringSoon(7));
            m.addAttribute("plans", subs.plans());
            m.addAttribute("members", users.activeMembers());
            m.addAttribute("methods", new PaymentMethod[]{PaymentMethod.CASH, PaymentMethod.UPI});
            return "subscriptions-staff";
        }
        m.addAttribute("mySubscriptions", subs.mine());
        m.addAttribute("current", subs.current(ctx.id()).orElse(null));
        long left = subs.daysLeft(ctx.id());
        m.addAttribute("daysLeft", left);
        m.addAttribute("canRenew", left <= PaymentService.RENEW_WINDOW_DAYS);
        m.addAttribute("plans", subs.activePlans());
        return "subscriptions-member";
    }

    @PostMapping("/plans")
    @PreAuthorize(SecurityConfig.ADMIN)
    public String createPlan(@RequestParam String name, @RequestParam int durationDays, @RequestParam BigDecimal price, RedirectAttributes ra) {
        subs.createPlan(name, durationDays, price);
        ra.addFlashAttribute("success", "Plan created");
        return "redirect:/subscriptions";
    }

    @PostMapping("/plans/{id}/toggle")
    @PreAuthorize(SecurityConfig.ADMIN)
    public String togglePlan(@PathVariable Long id) { subs.togglePlan(id); return "redirect:/subscriptions"; }

    /** Staff sells/renews a plan over the counter. Records a payment when the Payments module is on. */
    @PostMapping("/assign")
    @PreAuthorize(SecurityConfig.STAFF)
    public String assign(@RequestParam Long memberId, @RequestParam Long planId, @RequestParam(defaultValue = "CASH") PaymentMethod method,
                  @RequestParam(required = false) String reference, RedirectAttributes ra) {
        if (features.on(Feature.PAYMENTS)) payments.collect(memberId, planId, null, method, reference, null);
        else subs.assign(memberId, planId);
        ra.addFlashAttribute("success", "Subscription activated");
        return "redirect:/subscriptions";
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(SecurityConfig.STAFF)
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        subs.cancel(id);
        ra.addFlashAttribute("success", "Subscription cancelled");
        return "redirect:/subscriptions";
    }
}
