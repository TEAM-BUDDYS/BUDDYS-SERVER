package org.sopt.buddys.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI openAPI() {
    Info info = new Info()
        .title("BUDDYs API")
        .version("1.0")
        .description("38th Let's SOPT Buddys 서버 API 문서입니다.");

    SecurityScheme bearerScheme = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");

    return new OpenAPI()
        .addServersItem(new Server().url("/"))
        .info(info)
        .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }
}