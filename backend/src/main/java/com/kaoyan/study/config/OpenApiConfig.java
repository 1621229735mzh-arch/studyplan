package com.kaoyan.study.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 文档元信息。接口路径统一以 {@code /api} 开头。 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI studyOpenApi() {
        return new OpenAPI().info(new Info()
                .title("考研学习工作台 API")
                .version("v1")
                .description("阶段目标 → 周任务 → 今日安排 → 学习记录 → 进度反馈 → 手动调整。"
                        + "所有接口以 /api 为前缀，写操作需要携带 CSRF 请求头。"));
    }
}
