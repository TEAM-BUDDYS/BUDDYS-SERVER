package org.sopt.buddys.global.security.interceptor;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtProvider jwtProvider;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
        message,
        StompHeaderAccessor.class
    );

    if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }

    String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      throw new BadCredentialsException("WebSocket access token is required.");
    }

    String accessToken = authorization.substring(BEARER_PREFIX.length());
    Long userId = jwtProvider.extractUserId(accessToken)
        .orElseThrow(() -> new BadCredentialsException("Invalid WebSocket access token."));

    Authentication authentication = new UsernamePasswordAuthenticationToken(
        userId,
        null,
        Collections.emptyList()
    );
    accessor.setUser(authentication);

    return message;
  }
}
