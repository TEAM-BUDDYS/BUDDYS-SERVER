package org.sopt.buddys.global.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.security.interceptor.OriginValidationInterceptor;
import org.sopt.buddys.global.security.resolver.LoginUserArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private static final long MAX_AGE_SECS = 3600;
  private final LoginUserArgumentResolver loginUserArgumentResolver;
  private final OriginValidationInterceptor originValidationInterceptor;

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(loginUserArgumentResolver);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(originValidationInterceptor)
        .addPathPatterns("/api/v1/auth/reissue");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOriginPatterns(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(MAX_AGE_SECS);
  }
}
