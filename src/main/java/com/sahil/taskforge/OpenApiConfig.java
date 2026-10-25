package com.sahil.taskforge;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI taskForgeOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("TaskForge API")
                .description("REST API for creating, viewing, updating, and deleting tasks.")
                .version("1.0.0"));
    }
}
