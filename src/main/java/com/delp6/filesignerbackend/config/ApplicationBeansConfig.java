package com.delp6.filesignerbackend.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeansConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}