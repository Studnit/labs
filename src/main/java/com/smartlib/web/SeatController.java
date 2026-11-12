package com.smartlib.web;

import com.smartlib.config.SecurityConfig;
import com.smartlib.domain.Feature;
import com.smartlib.domain.Library;
import com.smartlib.feature.FeatureService;
import com.smartlib.repo.LibraryRepository;
import com.smartlib.security.CurrentUser;
import com.smartlib.service.SeatService;
import com.smartlib.service.SubscriptionService;
import com.smartlib.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/seats")
@PreAuthorize(SecurityConfig.NON_GUEST)

public class SeatController {
    private final SeatService seats;
    private final LibraryRepository libraries;
    private final CurrentUser ctx;
    private final FeatureService features;
    private final UserService users;
    private final SubscriptionService subscriptions;


    public SeatController(SeatService seats, LibraryRepository libraries, CurrentUser ctx, FeatureService features, UserService users, SubscriptionService subscriptions) {
        this.features = features;
        this.users = users;
        this.subscriptions = subscriptions;
        this.seats = seats;
        this.libraries = libraries;
        this.ctx = ctx;
    }
    @GetMapping
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                 @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                 @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime end, Model m) {
        Library lib = libraries.findById(ctx.requireLibraryId()).orElseThrow();
        LocalDate d = date == null ? LocalDate.now() : date;
        LocalTime s = start == null ? lib.getOpenTime() : start;
        LocalTime e = end == null ? (s.plusHours(4).isAfter(s) ? s.plusHours(4) : lib.getCloseTime()) : end;
        if (!e.isAfter(s)) e = s.plusMinutes(30).isAfter(s) ? s.plusMinutes(30) : s;
        m.addAttribute("date", d); m.addAttribute("start", s); m.addAttribute("end", e);
        m.addAttribute("seats", e.isAfter(s) ? seats.availability(d, s, e) : java.util.List.of());
        m.addAttribute("myBookings", seats.mine());
        boolean subsOn = features.on(Feature.SUBSCRIPTIONS);
        m.addAttribute("subscriptionsOn", subsOn);
        if (ctx.staff()) {
            m.addAttribute("upcoming", seats.upcoming());
            m.addAttribute("allocations", seats.activeAllocations());
            m.addAttribute("members", users.activeMembers());
            m.addAttribute("allSeats", seats.allSeats());
            m.addAttribute("locked", true);
        } else {
            boolean has = !subsOn || subscriptions.hasActive(ctx.id());
            m.addAttribute("hasSubscription", has);
            var al = seats.myAllocation().orElse(null);
            m.addAttribute("allocation", al);
            m.addAttribute("locked", al != null || !has);
        }
        return "seats";
    }

    @PostMapping("/book")
    @PreAuthorize("hasRole('MEMBER')")
    public String book(@RequestParam Long seatId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start, @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end,
                RedirectAttributes ra) {
        seats.book(seatId, date, start, end);
        ra.addFlashAttribute("success", "Seat booked for " + date + " " + start + "–" + end);
        return "redirect:/seats?date=" + date + "&start=" + start + "&end=" + end;
    }

    /** Member asks for a seat for their current subscription. */
    @PostMapping("/allocate-me")
    @PreAuthorize("hasRole('MEMBER')")
    public String allocateMe(RedirectAttributes ra) {
        var a = seats.allocateForMe();
        ra.addFlashAttribute("success", "Seat " + a.getSeat().getLabel() + " is reserved for you until " + a.getEndDate());
        return "redirect:/seats";
    }

    /** Staff assigns/changes a member's seat. Empty seatId = auto-pick. */
    @PostMapping("/allocate")
    @PreAuthorize(SecurityConfig.STAFF)
    public String allocate(@RequestParam Long memberId, @RequestParam(required = false) Long seatId, RedirectAttributes ra) {
        var a = seats.staffAllocate(memberId, seatId);
        ra.addFlashAttribute("success", "Seat " + a.getSeat().getLabel() + " allocated to " + a.getMember().getFullName());
        return "redirect:/seats";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        seats.cancel(id);
        ra.addFlashAttribute("success", "Booking cancelled");
        return "redirect:/seats";
    }

    @GetMapping("/manage")
    @PreAuthorize(SecurityConfig.STAFF)
    public String manage(Model m) { m.addAttribute("allSeats", seats.allSeats()); return "seats-manage"; }

    @PostMapping("/manage/add")
    @PreAuthorize(SecurityConfig.STAFF)
    public String add(@RequestParam String prefix, @RequestParam int count, @RequestParam(required = false) String zone, RedirectAttributes ra) {
        int n = seats.addSeats(prefix, count, zone);
        ra.addFlashAttribute("success", n + " seat(s) added");
        return "redirect:/seats/manage";
    }

    @PostMapping("/manage/{id}/toggle")
    @PreAuthorize(SecurityConfig.STAFF)
    public String toggle(@PathVariable Long id) { seats.toggle(id); return "redirect:/seats/manage"; }
}
