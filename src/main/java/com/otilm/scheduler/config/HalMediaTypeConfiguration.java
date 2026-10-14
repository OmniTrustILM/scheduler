package com.otilm.scheduler.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * Serves the Spring Data REST resources as {@code application/hal+json}, the media type they had before Spring Data
 * REST 5 put {@code application/vnd.hal+json} first.
 */
@Configuration
public class HalMediaTypeConfiguration implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.setDefaultMediaType(MediaTypes.HAL_JSON);
    }

    /** A request that accepts any media type gets the first one its converter lists. */
    @Override
    public void configureHttpMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof AbstractHttpMessageConverter<?> hal
                    && hal.getSupportedMediaTypes().contains(MediaTypes.HAL_JSON)) {
                hal.setSupportedMediaTypes(halJsonFirst(hal.getSupportedMediaTypes()));
            }
        }
    }

    private static List<MediaType> halJsonFirst(List<MediaType> mediaTypes) {
        List<MediaType> reordered = new ArrayList<>(mediaTypes);
        reordered.remove(MediaTypes.HAL_JSON);
        reordered.add(0, MediaTypes.HAL_JSON);
        return reordered;
    }
}
