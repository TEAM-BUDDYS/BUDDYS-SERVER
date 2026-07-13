package org.sopt.buddys.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID_KEY = "requestId";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString().substring(0, 8));

    String method = request.getMethod();
    String uri = request.getRequestURI();
    long start = System.currentTimeMillis();
    log.info("[REQUEST] {} {}", method, uri);

    try {
      filterChain.doFilter(request, response);
    } finally {
      log.info("[RESPONSE] {} {} → {} ({}ms)", method, uri, response.getStatus(), System.currentTimeMillis() - start);
      MDC.clear();
    }
  }
}