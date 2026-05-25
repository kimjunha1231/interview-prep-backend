package com.junha.interview.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Interview Prep API Specification")
                        .description("면접 질문 학습 및 AI 모의 면접 서비스의 백엔드 API 명세서입니다.")
                        .version("v1.0.0"));
    }
}
