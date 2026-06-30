package org.sopt.buddys.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final ObjectMapper objectMapper;

  private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
      "/api/v1/auth/login",
      "/api/v1/auth/reissue",
      "/api/v1/auth/signup",
      "/api/v1/auth/kakao",
      "/swagger-ui.html"
  );

  private static final List<String> PUBLIC_PREFIX_PATHS = List.of(
      "/v3/api-docs/",
      "/swagger-ui/"
  );

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return PUBLIC_EXACT_PATHS.contains(path) || PUBLIC_PREFIX_PATHS.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      Optional<Long> userId = jwtProvider.extractUserId(token);
      if (userId.isEmpty()) {
        sendUnauthorized(response);
        return;
      }
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(userId.get(), null, Collections.emptyList());
      auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    filterChain.doFilter(request, response);
  }

  private void sendUnauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), BaseResponse.failure(GlobalErrorCode.UNAUTHORIZED));
  }
}
