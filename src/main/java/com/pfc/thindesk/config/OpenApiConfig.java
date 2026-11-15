package com.pfc.thindesk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Título e descrição exibidos no Swagger UI (/swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI thindeskOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Thindesk - API do Dashboard Administrativo")
                .version("1.1.0")
                .description("Endpoints que alimentam o dashboard de chamados técnicos. "
                        + "Faça login no Thindesk antes de usar o \"Try it out\"."));
    }
}
