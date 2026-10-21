package com.cognia.catalogo.config;

import com.cognia.catalogo.service.ImagenService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final ImagenService imagenes;

    public WebConfig(ImagenService imagenes) { this.imagenes = imagenes; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(imagenes.directorio().toUri().toString().replaceAll("/?$", "/"));
    }
}