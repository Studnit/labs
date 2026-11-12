package com.smartlib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("http://localhost:8080") String baseUrl,
        @DefaultValue("./uploads") String uploadDir,
        @DefaultValue("false") boolean seedDemo,
        @DefaultValue("superadmin@smartlib.local") String superAdminEmail,
        @DefaultValue("ChangeMe#2026") String superAdminPassword,
        @DefaultValue("5") int reminderDays,
        @DefaultValue("true") boolean gatewayMock) {
}
