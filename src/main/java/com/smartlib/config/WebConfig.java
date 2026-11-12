package com.smartlib.config;

import com.smartlib.feature.FeatureInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration

public class WebConfig implements WebMvcConfigurer {
    private final FeatureInterceptor featureInterceptor;
    private final com.smartlib.security.ActiveUserInterceptor activeUser;


    public WebConfig(FeatureInterceptor featureInterceptor, com.smartlib.security.ActiveUserInterceptor activeUser) {
        this.activeUser = activeUser;
        this.featureInterceptor = featureInterceptor;
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activeUser).addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/webjars/**", "/error", "/login", "/logout");
        registry.addInterceptor(featureInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/webjars/**", "/error");
    }
}
