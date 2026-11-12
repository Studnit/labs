package com.smartlib.config;

import com.smartlib.security.LoginFailureHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** SpEL used with @PreAuthorize on staff-only endpoints. */
    public static final String STAFF = "hasAnyRole('LIBRARY_ADMIN','LIBRARIAN')";
    public static final String NON_GUEST = "hasAnyRole('MEMBER','LIBRARIAN','LIBRARY_ADMIN')";
    public static final String ADMIN = "hasRole('LIBRARY_ADMIN')";

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, LoginFailureHandler failureHandler) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/login", "/register", "/g/**", "/error", "/css/**", "/js/**", "/webjars/**", "/actuator/health/**").permitAll()
                .requestMatchers("/super/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/**").hasRole("LIBRARY_ADMIN")
                .anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/dashboard", true)
                .failureHandler(failureHandler).permitAll())
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()))
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .contentTypeOptions(c -> {})
                .httpStrictTransportSecurity(x -> x.includeSubDomains(true).maxAgeInSeconds(31536000))
                .addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Permissions-Policy", "camera=(self), microphone=(), geolocation=()"))
                .referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(c -> c.policyDirectives(
                    "default-src 'self'; img-src 'self' data: blob:; media-src 'self' blob:; object-src 'none'; base-uri 'self'; form-action 'self'; style-src 'self' 'unsafe-inline'; " +
                    "script-src 'self' 'unsafe-inline'; connect-src 'self' ws: wss:; font-src 'self'; frame-ancestors 'none'")));
        return http.build();
    }
}
