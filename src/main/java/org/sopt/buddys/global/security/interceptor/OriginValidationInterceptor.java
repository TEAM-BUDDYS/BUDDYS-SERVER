package org.sopt.buddys.global.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OriginValidationInterceptor implements HandlerInterceptor {
  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String origin = request.getHeader("Origin");
    if (origin == null || !isAllowedOrigin(origin)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }
    return true;
  }

  private boolean isAllowedOrigin(String origin) {
    String[] patterns = allowedOrigins.split(",");
    for (int i = 0; i < patterns.length; i++) {
      patterns[i] = patterns[i].trim();
    }
    return PatternMatchUtils.simpleMatch(patterns, origin);
  }
}
