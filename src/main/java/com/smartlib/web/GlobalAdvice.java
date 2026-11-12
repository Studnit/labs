package com.smartlib.web;

import com.smartlib.feature.FeatureDisabledException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.util.Map;

@ControllerAdvice

public class GlobalAdvice {

    private static boolean wantsJson(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return req.getRequestURI().contains("/api/") || "XMLHttpRequest".equals(req.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"));
    }

    @ExceptionHandler(FeatureDisabledException.class)
    Object featureDisabled(FeatureDisabledException e, HttpServletRequest req) {
        if (wantsJson(req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        // Signed-in users see the in-app page; anonymous visitors (public /g/{slug}) get the plain shell.
        boolean signedIn = req.getUserPrincipal() != null;
        ModelAndView mv = new ModelAndView(signedIn ? "error/feature-disabled" : "error", HttpStatus.FORBIDDEN);
        mv.addObject("feature", e.getFeature());
        mv.addObject("status", 403);
        return mv;
    }

    @ExceptionHandler(BusinessException.class)
    Object business(BusinessException e, HttpServletRequest req, RedirectAttributes ra) {
        if (wantsJson(req)) return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:" + safeBack(req);
    }

    /** Only same-site path/query from the Referer is used, never an external URL. */
    static String safeBack(HttpServletRequest req) {
        String ref = req.getHeader("Referer");
        try {
            if (ref != null) {
                URI u = URI.create(ref);
                if (u.getPath() != null && u.getPath().startsWith("/") && !u.getPath().startsWith("//"))
                    return u.getPath() + (u.getQuery() == null ? "" : "?" + u.getQuery());
            }
        } catch (IllegalArgumentException ignored) { }
        return "/dashboard";
    }
}
