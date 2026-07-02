package org.sopt.buddys.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private static final String JWT_SECURITY_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI openAPI() {
    Info info = new Info()
        .title("BUDDYs API")
        .version("1.0")
        .description("38th Let's SOPT Buddys 서버 API 문서입니다.");

    return new OpenAPI()
        .addServersItem(new Server().url("/"))
        .components(new Components()
            .addSecuritySchemes(
                JWT_SECURITY_SCHEME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            ))
        .addSecurityItem(new SecurityRequirement().addList(JWT_SECURITY_SCHEME))
        .info(info);
  }
}
