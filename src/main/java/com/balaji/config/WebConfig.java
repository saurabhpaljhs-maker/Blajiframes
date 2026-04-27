package com.balaji.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Loaded from env var UPLOAD_DIR
    // Dev:  /tmp/balaji-dev-uploads
    // Prod: /opt/balaji/uploads
    @Value("${app.upload.dir:/tmp/balaji-uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Serve uploaded photos at /uploads/**
        // Points to external folder — works after JAR build too
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600); // cache 1 hour

        // Standard classpath static resources
        registry.addResourceHandler("/css/**", "/js/**", "/images/**")
                .addResourceLocations("classpath:/static/");
    }
}
